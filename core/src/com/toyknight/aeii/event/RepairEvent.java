package com.toyknight.aeii.event;

import com.toyknight.aeii.AnimationDispatcher;
import com.toyknight.aeii.animator.MessageAnimator;
import com.toyknight.aeii.entity.GameCore;
import com.toyknight.aeii.entity.Tile;
import com.toyknight.aeii.utils.Language;

import java.io.Serializable;

/**
 * Created by toyknight on 5/25/2015.
 */
public class RepairEvent implements GameEvent, Serializable {

    private static final long serialVersionUID = 05252015L;

    private final int target_x;
    private final int target_y;

    public RepairEvent(int target_x, int target_y) {
        this.target_x = target_x;
        this.target_y = target_y;
    }

    @Override
    public boolean canExecute(GameCore game) {
        return game.getMap().getTile(target_x, target_y).isRepairable();
    }

    @Override
    public void execute(GameCore game, AnimationDispatcher animation_dispatcher) {
        Tile target_tile = game.getMap().getTile(target_x, target_y);
        game.setTile(target_tile.getRepairedTileIndex(), target_x, target_y);
        animation_dispatcher.submitAnimation(new MessageAnimator(Language.getText("LB_REPAIRED"), 0.5f));
    }

}