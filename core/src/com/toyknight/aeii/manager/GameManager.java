package com.toyknight.aeii.manager;

import com.toyknight.aeii.AnimationDispatcher;
import com.toyknight.aeii.animator.Animator;
import com.toyknight.aeii.entity.*;
import com.toyknight.aeii.listener.AnimationListener;
import com.toyknight.aeii.listener.GameManagerListener;
import com.toyknight.aeii.manager.events.GameEvent;
import com.toyknight.aeii.manager.events.TurnEndEvent;
import com.toyknight.aeii.utils.Recorder;
import com.toyknight.aeii.utils.UnitToolkit;

import java.util.*;

/**
 * @author toyknight  5/28/2015.
 */
public class GameManager implements AnimationDispatcher {

    public static final int STATE_SELECT = 0x1;
    public static final int STATE_MOVE = 0x2;
    public static final int STATE_REMOVE = 0x3;
    public static final int STATE_ACTION = 0x4;
    public static final int STATE_ATTACK = 0x5;
    public static final int STATE_SUMMON = 0x6;
    public static final int STATE_HEAL = 0x7;
    public static final int STATE_PREVIEW = 0x8;
    public static final int STATE_BUY = 0x9;

    private final Queue<GameEvent> event_queue;
    private final Queue<Animator> animation_queue;
    private Animator current_animation = null;

    private GameCore game;
    private GameManagerListener manager_listener;
    private final ArrayList<AnimationListener> animation_listeners;

    private int state;
    protected Unit selected_unit;
    protected Point last_position;

    private int[][] move_mark_map;
    private ArrayList<Point> move_path;
    private final HashSet<Point> movable_positions;
    private HashSet<Point> attackable_positions;

    private final int[] x_dir = {1, -1, 0, 0};
    private final int[] y_dir = {0, 0, 1, -1};

    public GameManager() {
        this.event_queue = new LinkedList<GameEvent>();
        this.animation_queue = new LinkedList<Animator>();
        this.animation_listeners = new ArrayList<AnimationListener>();
        this.movable_positions = new HashSet<Point>();
    }

    public void setGame(GameCore game) {
        this.game = game;
        this.state = STATE_SELECT;
        this.event_queue.clear();
        this.animation_queue.clear();
        this.current_animation = null;
        this.animation_listeners.clear();
    }

    public GameCore getGame() {
        return game;
    }

    public void setGameManagerListener(GameManagerListener listener) {
        this.manager_listener = listener;
    }

    public GameManagerListener getListener() {
        return manager_listener;
    }

    public boolean isProcessing() {
        return isAnimating() || !event_queue.isEmpty();
    }

    public void setState(int state) {
        if (state != this.state) {
            this.state = state;
            if (manager_listener != null) {
                manager_listener.onManagerStateChanged();
            }
        }
    }

    public int getState() {
        return state;
    }

    public void setSelectedUnit(Unit unit) {
        this.selected_unit = unit;
        this.movable_positions.clear();
        setLastPosition(new Point(unit.getX(), unit.getY()));
    }

    public void setLastPosition(Point position) {
        this.last_position = position;
    }

    public Point getLastPosition() {
        return last_position;
    }

    public void beginPreviewPhase(Unit target) {
        this.selected_unit = target;
        createMovablePositions();
        setState(STATE_PREVIEW);
    }

    public void cancelPreviewPhase() {
        setState(STATE_SELECT);
    }

    public void beginMovePhase() {
        createMovablePositions();
        setState(STATE_MOVE);
    }

    public void cancelMovePhase() {
        setState(STATE_SELECT);
    }

    public void beginAttackPhase() {
        setState(STATE_ATTACK);
        attackable_positions = createAttackablePositions(getSelectedUnit());
    }

    public void beginSummonPhase() {
        setState(STATE_SUMMON);
        attackable_positions = createAttackablePositions(getSelectedUnit());
    }

    public void beginHealPhase() {
        setState(STATE_HEAL);
        attackable_positions = createAttackablePositions(getSelectedUnit());
    }

    public void beginRemovePhase() {
        createMovablePositions();
        setState(STATE_REMOVE);
    }

    public void cancelActionPhase() {
        setState(STATE_ACTION);
    }

    public void onUnitMoveFinished() {
        switch (getState()) {
            case GameManager.STATE_MOVE:
                setState(GameManager.STATE_ACTION);
                break;
            case GameManager.STATE_REMOVE:
                GameHost.doStandbyUnit();
                break;
        }
    }

    public void onUnitActionFinished(Unit unit) {
        if (unit == null || unit.getCurrentHp() <= 0) {
            setState(GameManager.STATE_SELECT);
        } else {
            if (UnitToolkit.canMoveAgain(unit)) {
                setLastPosition(new Point(unit.getX(), unit.getY()));
                beginRemovePhase();
            } else {
                GameHost.doStandbyUnit();
            }
        }
    }

    public void submitGameEvent(GameEvent event) {
        event_queue.add(event);
    }

    private void dispatchGameEvents() {
        while (!isAnimating() && !event_queue.isEmpty()) {
            executeGameEvent(event_queue.poll());
        }
    }

    public void executeGameEvent(GameEvent event) {
        executeGameEvent(event, true);
    }

    public void executeGameEvent(GameEvent event, boolean record) {
        if (event.canExecute(getGame()) && !GameHost.isGameOver()) {
            event.execute(this);
            Point focus = event.getFocus(getGame());
            if (getGame().getCurrentPlayer().isLocalPlayer()) {
                if (focus != null && event instanceof TurnEndEvent) {
                    manager_listener.onMapFocusRequired(focus.x, focus.y);
                }
            } else {
                if (focus != null) {
                    manager_listener.onMapFocusRequired(focus.x, focus.y);
                }
            }
            if (record) {
                Recorder.submitGameEvent(event);
            }
        }
    }

    @Override
    public void addAnimationListener(AnimationListener listener) {
        this.animation_listeners.add(listener);
    }

    @Override
    public void submitAnimation(Animator animation) {
        if (current_animation == null) {
            current_animation = animation;
        } else {
            this.animation_queue.add(animation);
        }
    }

    @Override
    public void updateAnimation(float delta) {
        if (current_animation == null || current_animation.isAnimationFinished()) {
            boolean finish_flag = false;
            if (current_animation != null) {
                for (AnimationListener listener : animation_listeners) {
                    listener.animationCompleted(current_animation);
                }
                if (GameHost.isGameOver()) {
                    manager_listener.onGameOver();
                }
                finish_flag = true;
            }
            current_animation = animation_queue.poll();
            if (current_animation != null) {
                for (AnimationListener listener : animation_listeners) {
                    listener.animationStarted(current_animation);
                }
            } else {
                dispatchGameEvents();
                if (finish_flag && current_animation == null) {
                    manager_listener.onButtonUpdateRequested();
                }
            }
        } else {
            current_animation.update(delta);
        }
    }

    @Override
    public Animator getCurrentAnimation() {
        return current_animation;
    }

    @Override
    public boolean isAnimating() {
        return getCurrentAnimation() != null || !animation_queue.isEmpty();
    }

    private void createMoveMarkMap() {
        int width = getGame().getMap().getWidth();
        int height = getGame().getMap().getHeight();
        move_mark_map = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                move_mark_map[x][y] = Integer.MIN_VALUE;
            }
        }
    }

    public void createMovablePositions() {
        createMovablePositions(getSelectedUnit());
    }

    public HashSet<Point> createMovablePositions(Unit unit) {
        createMoveMarkMap();
        move_path = null;
        movable_positions.clear();
        int unit_x = unit.getX();
        int unit_y = unit.getY();
        int movement_point = unit.getCurrentMovementPoint();
        Point start_position = new Point(unit_x, unit_y);
        Step start_step = new Step(start_position, movement_point);
        Queue<Step> start_steps = new LinkedList<Step>();
        start_steps.add(start_step);
        createMovablePositions(start_steps, unit);
        return movable_positions;
    }

    private void createMovablePositions(Queue<Step> current_steps, Unit unit) {
        Queue<Step> next_steps = new LinkedList<Step>();
        while (!current_steps.isEmpty()) {
            Step current_step = current_steps.poll();
            int step_x = current_step.getPosition().x;
            int step_y = current_step.getPosition().y;
            if (current_step.getMovementPoint() > move_mark_map[step_x][step_y]) {
                move_mark_map[step_x][step_y] = current_step.getMovementPoint();
                if (getGame().canUnitMove(unit, step_x, step_y)) {
                    movable_positions.add(current_step.getPosition());
                }
            }
            for (int i = 0; i < 4; i++) {
                int next_x = current_step.getPosition().x + x_dir[i];
                int next_y = current_step.getPosition().y + y_dir[i];
                Point next = new Point(next_x, next_y);
                int current_mp = current_step.getMovementPoint();
                if (getGame().getMap().isWithinMap(next_x, next_y)) {
                    int mp_cost = UnitToolkit.getMovementPointCost(unit, getGame().getMap().getTile(next_x, next_y));
                    if (mp_cost <= current_mp && current_mp - mp_cost > move_mark_map[next_x][next_y]) {
                        Unit target_unit = game.getMap().getUnit(next_x, next_y);
                        if (getGame().canMoveThrough(unit, target_unit)) {
                            Step next_step = new Step(next, current_mp - mp_cost);
                            next_steps.add(next_step);
                        }
                    }
                }
            }
        }
        if (!next_steps.isEmpty()) {
            createMovablePositions(next_steps, unit);
        }
    }

    public Unit getSelectedUnit() {
        return selected_unit;
    }

    public int getMovementPointRemains(int dest_x, int dest_y) {
        Point dest_position = new Point(dest_x, dest_y);
        if (movable_positions.contains(dest_position)) {
            return move_mark_map[dest_x][dest_y];
        } else {
            return -1;
        }
    }

    public HashSet<Point> getMovablePositions() {
        return movable_positions;
    }

    public HashSet<Point> getAttackablePositions() {
        return attackable_positions;
    }

    public ArrayList<Point> getMovePath(int dest_x, int dest_y) {
        if (move_path == null || move_path.size() == 0) {
            createMovePath(dest_x, dest_y);
        } else {
            Point current_dest = move_path.get(move_path.size() - 1);
            if (dest_x != current_dest.x || dest_y != current_dest.y) {
                createMovePath(dest_x, dest_y);
            }
        }
        return move_path;
    }

    private void createMovePath(int dest_x, int dest_y) {
        move_path = new ArrayList<Point>();
        int start_x = getSelectedUnit().getX();
        int start_y = getSelectedUnit().getY();
        if (start_x != dest_x || start_y != dest_y) {
            Point dest_position = getGame().getMap().getPosition(dest_x, dest_y);
            if (movable_positions.contains(dest_position)) {
                createMovePath(dest_x, dest_y, start_x, start_y);
            }
        }
    }

    private void createMovePath(int current_x, int current_y, int start_x, int start_y) {
        move_path.add(0, new Point(current_x, current_y));
        if (current_x != start_x || current_y != start_y) {
            int next_x = 0;
            int next_y = 0;
            int next_mark = Integer.MIN_VALUE;
            for (int i = 0; i < 4; i++) {
                int tmp_next_x = current_x + x_dir[i];
                int tmp_next_y = current_y + y_dir[i];
                if (game.getMap().isWithinMap(tmp_next_x, tmp_next_y)) {
                    if (tmp_next_x == start_x && tmp_next_y == start_y) {
                        next_x = tmp_next_x;
                        next_y = tmp_next_y;
                        next_mark = Integer.MAX_VALUE;
                    } else {
                        int tmp_next_mark = move_mark_map[tmp_next_x][tmp_next_y];
                        if (tmp_next_mark > next_mark) {
                            next_x = tmp_next_x;
                            next_y = tmp_next_y;
                            next_mark = tmp_next_mark;
                        }
                    }
                }
            }
            createMovePath(next_x, next_y, start_x, start_y);
        }
    }

    public HashSet<Point> createAttackablePositions(Unit unit) {
        int unit_x = unit.getX();
        int unit_y = unit.getY();
        int min_ar = unit.getMinAttackRange();
        int max_ar = unit.getMaxAttackRange();
        HashSet<Point> attackable_positions = new HashSet<Point>();
        for (int ar = min_ar; ar <= max_ar; ar++) {
            for (int dx = -ar; dx <= ar; dx++) {
                int dy = dx >= 0 ? ar - dx : -ar - dx;
                if (game.getMap().isWithinMap(unit_x + dx, unit_y + dy)) {
                    attackable_positions.add(new Point(unit_x + dx, unit_y + dy));
                }
                if (dy != 0) {
                    if (game.getMap().isWithinMap(unit_x + dx, unit_y - dy)) {
                        attackable_positions.add(new Point(unit_x + dx, unit_y - dy));
                    }
                }
            }
        }
        if (getState() == STATE_HEAL) {
            attackable_positions.add(getGame().getMap().getPosition(unit.getX(), unit.getY()));
        }
        return attackable_positions;
    }

    public boolean hasEnemyWithinRange(Unit unit) {
        HashSet<Point> attackable_positions = createAttackablePositions(unit);
        for (Point point : attackable_positions) {
            if (getSelectedUnit().hasAbility(Ability.DESTROYER) && getGame().getMap().getUnit(point.x, point.y) == null
                    && getGame().getMap().getTile(point.x, point.y).isDestroyable()) {
                return true;
            }
            Unit target = getGame().getMap().getUnit(point.x, point.y);
            if (getGame().isEnemy(unit, target)) {
                return true;
            } else {
                if (target != null && target.hasAbility(Ability.LAST_POWER)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAllyCanHealWithinRange(Unit unit) {
        HashSet<Point> attackable_positions = createAttackablePositions(unit);
        for (Point point : attackable_positions) {
            Unit target = getGame().getMap().getUnit(point.x, point.y);
            if (getGame().canHeal(unit, target)) {
                return true;
            }
        }
        return getGame().canHeal(unit, unit);
    }

    public boolean hasTombWithinRange(Unit unit) {
        HashSet<Point> attackable_positions = createAttackablePositions(unit);
        for (Point point : attackable_positions) {
            if (getGame().getMap().isTomb(point.x, point.y) && getGame().getMap().getUnit(point.x, point.y) == null) {
                return true;
            }
        }
        return false;
    }

    public boolean canSelectedUnitAct() {
        return !getSelectedUnit().hasAbility(Ability.HEAVY_MACHINE) || getSelectedUnit().isAt(last_position.x, last_position.y);
    }

    public boolean canSelectedUnitMove(int dest_x, int dest_y) {
        Point dest = getGame().getMap().getPosition(dest_x, dest_y);
        return getMovablePositions().contains(dest)
                && getGame().canUnitMove(getSelectedUnit(), dest_x, dest_y)
                && getGame().isUnitAccessible(getSelectedUnit());
    }

    public boolean isMovablePosition(int map_x, int map_y) {
        return getMovablePositions().contains(getGame().getMap().getPosition(map_x, map_y));
    }

}
