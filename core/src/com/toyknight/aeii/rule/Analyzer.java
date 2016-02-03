package com.toyknight.aeii.rule;

import static com.toyknight.aeii.rule.Rule.Entry.*;

import com.toyknight.aeii.entity.GameCore;
import com.toyknight.aeii.entity.Player;
import com.toyknight.aeii.entity.Unit;

/**
 * @author toyknight 1/10/2016.
 */
public class Analyzer {

    private final GameCore game;

    public Analyzer(GameCore game) {
        this.game = game;
    }

    public GameCore getGame() {
        return game;
    }

    public boolean isTeamDestroyed(int team) {
        boolean unit_check = true;
        if (getGame().getRule().getBoolean(ENEMY_CLEAR)) {
            for (Unit unit : getGame().getMap().getUnits()) {
                if (unit.getTeam() == team) {
                    unit_check = false;
                    break;
                }
            }
        }
        boolean castle_check = true;
        if (getGame().getRule().getBoolean(CASTLE_CLEAR)) {
            castle_check = getGame().getMap().getCastleCount(team) <= 0;
        }
        return unit_check && castle_check;
    }

    public int getWinnerAlliance() {
        int alliance = -1;
        for (int team = 0; team < 4; team++) {
            Player player = getGame().getPlayer(team);
            if (alliance == -1) {
                if (getGame().isPlayerAvailable(team)) {
                    alliance = player.getAlliance();
                }
            } else {
                if (getGame().isPlayerAvailable(team) && player.getAlliance() != alliance) {
                    alliance = -1;
                    break;
                }
            }
        }
        return alliance;
    }

}
