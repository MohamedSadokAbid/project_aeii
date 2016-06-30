package net.toyknight.aeii.animation;

import net.toyknight.aeii.entity.Position;
import net.toyknight.aeii.screen.MapCanvas;

import java.util.HashSet;

/**
 * @author toyknight 4/21/2015.
 */
public class MapAnimator extends Animator {

    private final HashSet<Position> locations = new HashSet<Position>();
    private static MapCanvas canvas;

    public MapAnimator() {
    }

    public MapAnimator(int x, int y) {
        if (x >= 0 && y >= 0) {
            this.addLocation(x, y);
        }
    }

    protected MapCanvas getCanvas() {
        return canvas;
    }

    protected int ts() {
        return getCanvas().ts();
    }

    public final void addLocation(int x, int y) {
        if (getCanvas().getMap().isWithinMap(x, y)) {
            this.locations.add(getCanvas().getMap().getPosition(x, y));
        }
    }

    public final boolean hasLocation(int x, int y) {
        return locations.contains(new Position(x, y));
    }

    public static void setCanvas(MapCanvas canvas) {
        MapAnimator.canvas = canvas;
    }

}
