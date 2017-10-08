package nxt.db.quicksync;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.db.sql.Db;
import nxt.util.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Automagically creates binary dumps at configurable intervals
 */
public class Checkpoints implements Listener<Block> {

    private static final Logger logger = LoggerFactory.getLogger(Checkpoints.class);
    private static final boolean enabled = Nxt.getBooleanProperty("nxt.createCheckpoints", false);
    private static final long interval = Long.parseLong(Nxt.getStringProperty("nxt.checkpointInterval", String.valueOf(Long.MAX_VALUE)));
    private static final String path = Nxt.getStringProperty("nxt.checkpointPath", "./checkpoints");

    public static void init() {
        if (enabled) {

            logger.info("Checkpoints are enabled");
            File outputDir = new File(path);
            if (!outputDir.exists()) {
                logger.info("Creating output dir " + outputDir.getAbsolutePath());
                if (!outputDir.mkdirs())
                    logger.warn("Unable to create output dir");
            }
            Nxt.getBlockchainProcessor().addListener(new Checkpoints(), BlockchainProcessor.Event.BLOCK_PUSHED);

        }
    }

    @Override
    public void notify(Block block) {
        if (block.getHeight() % interval == 0) {
            long height = block.getHeight();
            logger.info("Creating checkpoint " + height);
            Path op = Paths.get(path, "checkPoint-" + height + ".dump");
            try {
                CreateBinDump.dump(op.toAbsolutePath().toString());
            } catch (IOException | InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException | URISyntaxException e) {
                logger.error("Unable to create checkpoint", e);
            }
        }
    }
}
