package com.bitdecay.jump;

import com.bitdecay.jump.geom.BitPointInt;
import com.bitdecay.jump.geom.BitRectangle;
import com.bitdecay.jump.level.Level;
import org.junit.Assert;
import org.junit.Test;

public class TestCollisions {

    @Test
    public void testStaticBoxToDynamicBoxCollision(){
        BitWorld world = new BitWorld();
        Level l = new Level(1);
        l.gridOffset = new BitPointInt(0, 0);
        world.setLevel(l);
        world.setGravity(0, -1);

        BitBody staticBody = new BitBody();
        staticBody.bodyType = BodyType.STATIC;
        staticBody.aabb = new BitRectangle(-50, 0, 100, 1);
        world.addBody(staticBody);

        BitBody dynamicBody = new BitBody();
        dynamicBody.bodyType = BodyType.DYNAMIC;
        dynamicBody.aabb = new BitRectangle(0, 20, 10, 10);
        world.addBody(dynamicBody);

        final CollisionHelper c = new CollisionHelper();
        // listen to collisions
        world.addCollisionListener((a, b) -> {
            c.a = a;
            c.b = b;
        });

        for (int i = 0; i < 100; i++){
            world.step(0.1f);
        }

        Assert.assertTrue("Dynamic body should have moved down from its original y point", dynamicBody.aabb.xy.y < 20);
        Assert.assertTrue("Dynamic body should not have moved past the static body's y point " + dynamicBody.aabb, dynamicBody.aabb.xy.y > 0);

        Assert.assertNotNull("The collision should have happened between the static and dynamic bodies", c.a);
    }

    public class CollisionHelper {
        public BitBody a = null;
        public BitBody b = null;
    }
}
