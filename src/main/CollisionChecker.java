package main;

import entity.Entity;
import java.awt.Rectangle;

public class CollisionChecker {

    GamePanel gp;

    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    // -----------------------------------------------
    // TILE COLLISION
    // All tile lookups go through isSolid() which
    // clamps coordinates — never throws OutOfBounds
    // -----------------------------------------------
    public void checkTile(Entity entity) {
        int entityLeftX   = entity.worldX + entity.solidArea.x;
        int entityRightX  = entity.worldX + entity.solidArea.x + entity.solidArea.width;
        int entityTopY    = entity.worldY + entity.solidArea.y;
        int entityBottomY = entity.worldY + entity.solidArea.y + entity.solidArea.height;

        int leftCol   = entityLeftX   / gp.tileSize;
        int rightCol  = entityRightX  / gp.tileSize;
        int topRow    = entityTopY    / gp.tileSize;
        int bottomRow = entityBottomY / gp.tileSize;

        switch (entity.direction) {
            case "up" -> {
                topRow = (entityTopY - entity.speed) / gp.tileSize;
                if (gp.tileManager.isSolid(leftCol,  topRow) ||
                        gp.tileManager.isSolid(rightCol, topRow))
                    entity.collisionOn = true;
            }
            case "down" -> {
                bottomRow = (entityBottomY + entity.speed) / gp.tileSize;
                if (gp.tileManager.isSolid(leftCol,  bottomRow) ||
                        gp.tileManager.isSolid(rightCol, bottomRow))
                    entity.collisionOn = true;
            }
            case "left" -> {
                leftCol = (entityLeftX - entity.speed) / gp.tileSize;
                if (gp.tileManager.isSolid(leftCol, topRow) ||
                        gp.tileManager.isSolid(leftCol, bottomRow))
                    entity.collisionOn = true;
            }
            case "right" -> {
                rightCol = (entityRightX + entity.speed) / gp.tileSize;
                if (gp.tileManager.isSolid(rightCol, topRow) ||
                        gp.tileManager.isSolid(rightCol, bottomRow))
                    entity.collisionOn = true;
            }
        }
    }

    // -----------------------------------------------
    // OBJECT COLLISION
    // Returns index of hit object, 999 if none
    // -----------------------------------------------
    public int checkObject(Entity entity, boolean isPlayer) {
        int index = 999;
        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] == null) continue;

            Rectangle er = new Rectangle(
                    entity.worldX + entity.solidArea.x,
                    entity.worldY + entity.solidArea.y,
                    entity.solidArea.width, entity.solidArea.height
            );
            Rectangle or2 = new Rectangle(
                    gp.object[i].worldX + gp.object[i].solidArea.x,
                    gp.object[i].worldY + gp.object[i].solidArea.y,
                    gp.object[i].solidArea.width, gp.object[i].solidArea.height
            );

            switch (entity.direction) {
                case "up"    -> er.y -= entity.speed;
                case "down"  -> er.y += entity.speed;
                case "left"  -> er.x -= entity.speed;
                case "right" -> er.x += entity.speed;
            }

            if (er.intersects(or2)) {
                if (gp.object[i].collision) entity.collisionOn = true;
                if (isPlayer) index = i;
            }
        }
        return index;
    }

    // -----------------------------------------------
    // ENTITY-TO-ENTITY COLLISION
    // Returns index of hit target, 999 if none
    // -----------------------------------------------
    public int checkEntity(Entity entity, Entity[] targets) {
        int index = 999;
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == null) continue;

            Rectangle er = new Rectangle(
                    entity.worldX + entity.solidArea.x,
                    entity.worldY + entity.solidArea.y,
                    entity.solidArea.width, entity.solidArea.height
            );
            Rectangle tr = new Rectangle(
                    targets[i].worldX + targets[i].solidArea.x,
                    targets[i].worldY + targets[i].solidArea.y,
                    targets[i].solidArea.width, targets[i].solidArea.height
            );

            switch (entity.direction) {
                case "up"    -> er.y -= entity.speed;
                case "down"  -> er.y += entity.speed;
                case "left"  -> er.x -= entity.speed;
                case "right" -> er.x += entity.speed;
            }

            if (er.intersects(tr)) {
                entity.collisionOn = true;
                index = i;
            }
        }
        return index;
    }
}
