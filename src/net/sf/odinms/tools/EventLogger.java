package net.sf.odinms.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a tool for writing to and reading from a certain fixed file
 * that appends to the end of that file when writing. Used for logging
 * events for later reference.
 *
 * None of the methods that this object possesses are nullable.
 */
public class EventLogger {
    private final Path path;

    /**
     * Creates a new instance of EventLogger that writes to
     * the filepath specified by <code>pathString</code>.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param pathString String representing the filepath to write to.
     *
     * @throws InvalidPathException when <code>pathString</code> does
     * not represent a valid path.
     */
    EventLogger(String pathString) {
        path = Paths.get(pathString);
    }

    /**
     * Creates a new instance of EventLogger that writes to
     * the non-null filepath specified by <code>path</code>.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param path <code>Path</code> representing the filepath to write to.
     *
     * @throws NullPointerException when <code>path == null</code>.
     */
    EventLogger(Path path) {
        if (path == null) {
            throw new NullPointerException("The Path given to the EventLogger constructor may not be null.");
        }
        this.path = path;
    }

    /**
     * Writes a list of lines to the event log file represented
     * by this object. This method appends the lines to the end of the log.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param lines A list of the lines (as <code>String</code>s) to write
     *              to the event log.
     * @return <code>true</code> on success, <code>false</code> on failure.
     */
    public boolean write(List<String> lines) {
        try {
            Files.write(
                path,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            );
        } catch (IOException ioe) {
            System.err.println("Exception occured during an EventLogger.write(): " + ioe);
            return false;
        }
        return true;
    }

    /**
     * Writes bytes to the event log file represented
     * by this object. This method appends the bytes to the end
     * of the file.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param data A byte array representing the data to be written to
     *             the event log.
     * @return <code>true</code> on success, <code>false</code> on failure.
     */
    public boolean write(byte[] data) {
        try {
            Files.write(
                path,
                data,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            );
        } catch (IOException ioe) {
            System.err.println("Exception occured during an EventLogger writing operation: " + ioe);
            return false;
        }
        return true;
    }

    /**
     * Writes a single line to the event log file represented
     * by this object, calling <code>write()</code> with
     * a singleton list. This method appends the line to the
     * end of the file.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @param line A line (as a <code>String</code>) to write to the event log.
     * @return <code>true</code> on success, <code>false</code> on failure.
     */
    public boolean writeln(String line) {
        return write(Collections.singletonList(line));
    }

    /**
     * Deletes the file represented by this object
     * from the filesystem.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @return <code>true</code> on success, <code>false</code> on failure
     * or if the file does not exist.
     */
    public boolean delete() {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ioe) {
            System.err.println("Exception occured during an EventLogger delete operation: " + ioe);
            return false;
        }
    }

    /**
     * Returns the time (in milliseconds since the Unix epoch)
     * that the file represented by this object was last modified.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @return Time in milliseconds, or -1 upon failure.
     */
    public long lastModifiedTime() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ioe) {
            System.err.println(
                "Exception occured during an EventLogger get last modified time operation: " + ioe
            );
            return -1L;
        }
    }

    /**
     * Returns the size (in bytes) of the file represented by this object.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @return Size in bytes, or -1 upon failure.
     */
    public long fileSize() {
        try {
            return Files.size(path);
        } catch (IOException ioe) {
            System.err.println("Exception occured during an EventLogger get filesize operation: " + ioe);
            return -1L;
        }
    }

    /**
     * Whether or not the file represented by this object
     * currently exists on the filesystem.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @return <code>true</code> if the file exists,
     * <code>false</code> if the file does not exist or
     * the check fails.
     */
    public boolean fileExists() {
        try {
            return Files.exists(path);
        } catch (Exception e) {
            System.err.println("Exception occured during an EventLogger check for existence operation: " + e);
            return false;
        }
    }

    /**
     * Gets a <code>Stream</code> of <code>String</code>s that reads line by line (lazily)
     * from the event log file represented by this object. NOTE: The <code>Stream</code>
     * must be <code>.close()</code>ed after use, so it's best to call this method
     * in the parenthesized header of a <code>try</code> block. In addition,
     * this method returns an empty <code>Optional</code> if the initial reading/obtaining
     * of the <code>Stream</code> fails.
     *
     * <ul>
     * <li>pure?: false</li>
     * </ul>
     *
     * @return An <code>Optional</code> containing a lazy stream of lines from the event
     * log file, or an empty <code>Optional</code> if the initial read failed.
     */
    public Optional<Stream<String>> getLines() {
        try {
            return Optional.of(Files.lines(path, StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            System.err.println("Exception occured during an EventLogger read operation: " + ioe);
            return Optional.empty();
        }
    }

    /**
     * Returns a direct reference to the non-null filepath represented by this object.
     *
     * <ul>
     * <li>pure?: false</li>
     * <li>nullable?: false</li>
     * </ul>
     *
     * @return The non-null filepath represented by this object.
     */
    public Path getPath() {
        return path;
    }
}
