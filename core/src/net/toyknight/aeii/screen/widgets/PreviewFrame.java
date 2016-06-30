package net.toyknight.aeii.screen.widgets;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import net.toyknight.aeii.ResourceManager;
import net.toyknight.aeii.entity.Status;
import net.toyknight.aeii.renderer.BorderRenderer;
import net.toyknight.aeii.renderer.UnitRenderer;
import net.toyknight.aeii.utils.UnitFactory;

/**
 * @author toyknight 6/17/2016.
 */
public class PreviewFrame extends Widget {

    private final int ts;

    private int tile_index;
    private int unit_index;

    private int status;

    public PreviewFrame(int ts) {
        this(ts, 0, 0, -1);
    }

    public PreviewFrame(int ts, int tile_index, int unit_index, int status) {
        this.ts = ts;
        this.tile_index = tile_index;
        this.unit_index = unit_index;
        this.status = status;
    }

    public void setTileIndex(int tile_index) {
        this.tile_index = tile_index;
    }

    public void setUnitIndex(int unit_index) {
        this.unit_index = unit_index;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public float getPrefWidth() {
        return ts;
    }

    @Override
    public float getPrefHeight() {
        return ts;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX(), y = getY(), width = getWidth(), height = getHeight();
        batch.draw(ResourceManager.getTileTexture(tile_index), x, y, width, height);
        BorderRenderer.drawBorder(batch, x, y, width, height);
        if (unit_index >= 0) {
            UnitRenderer.drawUnit_(batch, UnitFactory.getSample(unit_index), x, y, 0, ts);
        }

        int sw = ts * ResourceManager.getStatusTexture(0).getRegionWidth() / 24;
        int sh = ts * ResourceManager.getStatusTexture(0).getRegionHeight() / 24;
        switch (status) {
            case Status.BLINDED:
                batch.draw(ResourceManager.getStatusTexture(3), x, y + height - sh, sw, sh);
                break;
            case Status.INSPIRED:
                batch.draw(ResourceManager.getStatusTexture(1), x, y + height - sh, sw, sh);
                break;
            case Status.POISONED:
                batch.draw(ResourceManager.getStatusTexture(0), x, y + height - sh, sw, sh);
                break;
            case Status.SLOWED:
                batch.draw(ResourceManager.getStatusTexture(2), x, y + height - sh, sw, sh);
                break;
        }
        batch.flush();
    }

}
