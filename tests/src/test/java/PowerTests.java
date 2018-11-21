import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.PowerBlocks;
import io.anuke.mindustry.content.blocks.ProductionBlocks;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.blocks.production.SolidPump;
import io.anuke.mindustry.world.modules.PowerModule;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import static io.anuke.mindustry.Vars.threads;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PowerTests{

    @BeforeAll
    static void initializeDependencies(){
        Vars.content = new ContentLoader();
        Vars.content.load();
    }

    @BeforeEach
    void initTest(){
    }

    /**
     * Creates a fake tile on the given location using the given floor and block.
     * @param x     The X coordinate.
     * @param y     The y coordinate.
     * @param floor The floor.
     * @param block The block on the tile.
     * @return The created tile or null in case of exceptions.
     */
    private static Tile createFakeTile(int x, int y, Block block){
        try{
            Tile tile = new Tile(x, y);
            Field field = Tile.class.getDeclaredField("wall");
            field.setAccessible(true);
            field.set(tile, block);
            field = Tile.class.getDeclaredField("floor");
            field.setAccessible(true);
            field.set(tile, (Floor)Blocks.sand);
            tile.entity = block.newEntity();
            tile.entity.power = new PowerModule();
            return tile;
        }catch(Exception ex){
            return null;
        }
    }
    private static final float epsilon = 0.00001f;

    /** Makes sure calculations are accurate for the case where produced power = consumed power. */
    @Test
    void test_balancedPower(){
        PowerGraph powerGraph = new PowerGraph();

        // Create one water extractor (5.4 power/Second = 0.09/tick)
        Tile waterExtractorTile = createFakeTile(0, 0, ProductionBlocks.waterExtractor);
        powerGraph.add(waterExtractorTile);

        // Create 20 small solar panels (20*0.27=5.4 power/second = 0.09/tick)
        List<Tile> solarPanelTiles = new LinkedList<>();
        for(int counter = 0; counter < 20; counter++){
            Tile solarPanelTile = createFakeTile( 2 + counter / 2, counter % 2, PowerBlocks.solarPanel);
            powerGraph.add(solarPanelTile);
            solarPanelTiles.add(solarPanelTile);
        }

        float powerNeeded = powerGraph.getPowerNeeded();
        float powerProduced = powerGraph.getPowerProduced();

        // If these lines fail, you probably changed power production/consumption and need to adapt this test
        // OR their implementation is corrupt.
        // TODO: Create fake blocks which are independent of such changes
        assertEquals(powerNeeded, 0.09f, epsilon);
        assertEquals(powerProduced, 0.09f, epsilon);
        // Note: The assertions above induce that powerNeeded = powerProduced (with floating point inaccuracy)

        // Distribute power and make sure the water extractor is powered
        powerGraph.distributePower(powerNeeded, powerProduced);
        assertEquals(waterExtractorTile.entity.power.satisfaction, 1.0f, epsilon);
    }

    /** Makes sure there are no problems with zero production. */
    @Test
    void test_noProducers(){
        PowerGraph powerGraph = new PowerGraph();

        Tile waterExtractorTile = createFakeTile(0, 0, ProductionBlocks.waterExtractor);
        powerGraph.add(waterExtractorTile);

        float powerNeeded = powerGraph.getPowerNeeded();
        float powerProduced = powerGraph.getPowerProduced();

        assertEquals(powerGraph.getPowerNeeded(), 0.09f, epsilon);
        assertEquals(powerGraph.getPowerProduced(), 0.0f, epsilon);

        powerGraph.distributePower(powerNeeded, powerProduced);
        assertEquals(waterExtractorTile.entity.power.satisfaction, 0.0f, epsilon);
    }

    /** Makes sure there are no problems with zero consumers. */
    @Test
    void test_noConsumers(){
        PowerGraph powerGraph = new PowerGraph();

        Tile solarPanelTile = createFakeTile( 0, 0, PowerBlocks.solarPanel);
        powerGraph.add(solarPanelTile);

        float powerNeeded = powerGraph.getPowerNeeded();
        float powerProduced = powerGraph.getPowerProduced();

        assertEquals(powerGraph.getPowerNeeded(), 0.0f, epsilon);
        assertEquals(powerGraph.getPowerProduced(), 0.0045f, epsilon);

        powerGraph.distributePower(powerNeeded, powerProduced);
    }
}
