package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import com.turn.ttorrent.common.Torrent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MakeTorrentTask extends JobBase {

    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id,
            Path root, JobDataMap jobData) throws JobExecutionException {
        try {
            makeTorrentFile(repo.getPublishPathForId(id),
                    repo.getPublishPathForId(id).resolve(id + ".torrent"),
                    jobData.getString("torrent.tracker"));
        } catch (NoSuchAlgorithmException | InterruptedException |
                IOException | URISyntaxException ex) {
            throw new JobExecutionException("Issue creating torrent", ex);
        }
    }

    protected void makeTorrentFile(Path toShare, Path torrentFile, String tracker) throws NoSuchAlgorithmException, InterruptedException, IOException, URISyntaxException {
        Torrent torrent = Torrent.create(
                toShare.toFile(), listContent(toShare),
                new URI(tracker), "dundry");
        torrent.save(torrentFile.toFile());
    }

    private List<File> listContent(final Path parent) throws IOException {

        final List<File> files = new ArrayList<>();

        Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    public static void main(String... args) throws NoSuchAlgorithmException, InterruptedException, IOException, URISyntaxException {
        MakeTorrentTask m = new MakeTorrentTask();
        
        m.makeTorrentFile(Paths.get("/tmp/test-dir"), Paths.get("/tmp/test.torrent"), "http://example.com/tracker");
    }
}
