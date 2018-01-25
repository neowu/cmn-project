package core.aws.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;

/**
 * @author neo
 */
public final class Tarball {
    private final Path sourceDir;
    private final URI basePath;

    public Tarball(Path sourceDir) {
        this.sourceDir = sourceDir;
        basePath = sourceDir.toUri();
    }

    public void archive(File outputFile) throws IOException {
        try (TarArchiveOutputStream output = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(java.nio.file.Files.newOutputStream(outputFile.toPath()))))) {
            archive(sourceDir, output);
            output.flush();
        }
    }

    private void archive(Path path, TarArchiveOutputStream output) throws IOException {
        if (isDirectory(path)) {
            archiveFolder(path, output);
        } else {
            TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), basePath.relativize(path.toUri()).getPath());   // must covert path to URI to make it compatible with Linux
            output.putArchiveEntry(entry);
            output.write(readAllBytes(path));
            output.closeArchiveEntry();
        }
    }

    private void archiveFolder(Path path, TarArchiveOutputStream output) throws IOException {
        File[] files = path.toFile().listFiles();
        if (files != null) {
            if (files.length == 0) {
                // add empty folder
                TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), basePath.relativize(path.toUri()).getPath());
                output.putArchiveEntry(entry);
                output.closeArchiveEntry();
            } else {
                for (File childFile : files) {
                    archive(childFile.toPath(), output);
                }
            }
        }
    }
}
