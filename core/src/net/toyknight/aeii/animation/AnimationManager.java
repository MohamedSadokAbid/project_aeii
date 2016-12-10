package net.toyknight.aeii.animation;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import net.toyknight.aeii.GameContext;
import net.toyknight.aeii.entity.Position;
import net.toyknight.aeii.entity.Unit;
import net.toyknight.aeii.manager.AnimationDispatcher;
import net.toyknight.aeii.manager.AnimationListener;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author toyknight 11/1/2015.
 */
public class AnimationManager implements AnimationDispatcher {

    private AnimationListener listener;

    private final Queue<Animator> animation_queue;
    private Animator current_animation = null;

    public AnimationManager() {
        this.animation_queue = new LinkedList<Animator>();
    }

    @Override
    public void setListener(AnimationListener listener) {
        this.listener = listener;
    }

    @Override
    public void reset() {
        animation_queue.clear();
        current_animation = null;
    }

    @Override
    public void submitHpChangeAnimation(ObjectMap<Position, Integer> change_map, ObjectSet<Unit> units) {
        submitAnimation(new HpChangeAnimator(change_map, units));
    }

    @Override
    public void submitHpChangeAnimation(Unit unit, int change) {
        submitAnimation(new HpChangeAnimator(unit, change));
    }

    @Override
    public void submitMessageAnimation(String message, float delay) {
        submitAnimation(new MessageAnimator(message, delay));
    }

    @Override
    public void submitMessageAnimation(String message_upper, String message_lower, float delay) {
        submitAnimation(new MessageAnimator(message_upper, message_lower, delay));
    }

    @Override
    public void submitSummonAnimation(Unit summoner, int target_x, int target_y) {
        submitAnimation(new SummonAnimator(summoner, target_x, target_y));
    }

    @Override
    public void submitUnitLevelUpAnimation(Unit unit) {
        submitAnimation(new UnitLevelUpAnimator(unit));
    }

    @Override
    public void submitDustAriseAnimation(int map_x, int map_y) {
        submitAnimation(new DustAriseAnimator(map_x, map_y));
    }

    @Override
    public void submitDustAriseAnimation(ObjectSet<Position> positions) {
        submitAnimation(new DustAriseAnimator(positions));
    }

    @Override
    public void submitUnitAttackAnimation(Unit attacker, Unit target, int damage) {
        submitAnimation(new UnitAttackAnimator(attacker, target, damage));
    }

    @Override
    public void submitUnitAttackAnimation(Unit target, int damage) {
        submitAnimation(new UnitAttackAnimator(target, damage));
    }

    @Override
    public void submitUnitAttackAnimation(int target_x, int target_y) {
        submitAnimation(new UnitAttackAnimator(target_x, target_y));
    }

    @Override
    public void submitUnitAttackAnimation(Unit attacker, int target_x, int target_y) {
        submitAnimation(new UnitAttackAnimator(attacker, target_x, target_y));
    }

    @Override
    public void submitUnitSparkAnimation(Unit unit) {
        submitAnimation(new UnitSparkAnimator(unit));
    }

    @Override
    public void submitUnitSparkAnimation(ObjectSet<Unit> units) {
        submitAnimation(new UnitSparkAnimator(units));
    }

    @Override
    public void submitUnitMoveAnimation(Unit unit, Array<Position> path) {
        submitAnimation(new UnitMoveAnimator(unit, path));
    }

    @Override
    public void submitReinforceAnimation(Array<Unit> reinforcements, int from_x, int from_y) {
        submitAnimation(new ReinforceAnimator(reinforcements, from_x, from_y));
    }

    @Override
    public void submitCrystalStealAnimation(int map_x, int map_y, int target_x, int target_y) {
        submitAnimation(new CrystalStealAnimator(map_x, map_y, target_x, target_y));
    }

    @Override
    public void submitFlyOverAnimation(Unit flier, Unit target, int start_x, int start_y) {
        submitAnimation(new FlyOverAnimator(flier, target, start_x, start_y));
    }

    @Override
    public void submitUnitCarryAnimation(Unit carrier, Unit target, int dest_x, int dest_y) {
        submitAnimation(new UnitCarryAnimator(carrier, target, dest_x, dest_y));
    }

    @Override
    public void submitHavensFuryAnimation(Unit target) {
        submitAnimation(new HavensFuryAnimator(target));
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
        if (current_animation == null) {
            current_animation = animation_queue.poll();
        } else {
            if (current_animation.isAnimationFinished()) {
                if (animation_queue.isEmpty()) {
                    current_animation = null;
                    listener.onAnimationFinished();
                } else {
                    current_animation = animation_queue.poll();
                }
            } else {
                current_animation.update(delta);
            }
        }
    }

    @Override
    public Animator getCurrentAnimation() {
        return current_animation;
    }

    @Override
    public boolean isAnimating() {
        return current_animation != null || animation_queue.size() > 0;
    }

}
