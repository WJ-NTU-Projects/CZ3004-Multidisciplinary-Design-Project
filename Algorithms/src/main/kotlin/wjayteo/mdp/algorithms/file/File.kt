package wjayteo.mdp.algorithms.file

import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.*


class File {
    companion object {
        const val CONNECTION = "connection.txt"
        const val ARENA_PREFERENCES = "arena_preferences.txt"

        @Throws(IOException::class)
        fun readDataFile(file: String): List<String> {
            val path: Path = Paths.get("data/$file")
            createDirectoryIfNotExist(Paths.get("data"))
            createFileIfNotExist(path)
            return Files.readAllLines(path)
        }

        fun readFile(file: String): List<String> {
            val path: Path = Paths.get(file)
            return Files.readAllLines(path)
        }

        @Throws(IOException::class)
        fun replaceDataFileContent(file: String, text: String) {
            val path: Path = Paths.get("data/$file")
            createDirectoryIfNotExist(Paths.get("data"))
            createFileIfNotExist(path)
            FileChannel.open(path, StandardOpenOption.WRITE).truncate(0)
            Files.write(path, text.toByteArray())
        }

        @Throws(IOException::class)
        fun replaceFileContent(file: String, text: String) {
            if (file.contains("connection", ignoreCase = true)) return
            var fileName: String = file
            if (!file.contains(".txt")) fileName += ".txt"
            val path: Path = Paths.get(fileName)
            createFileIfNotExist(path)
            FileChannel.open(path, StandardOpenOption.WRITE).truncate(0)
            Files.write(path, text.toByteArray())
        }

        @Throws(IOException::class)
        private fun createDirectoryIfNotExist(path: Path) {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(path)
        }

        @Throws(IOException::class)
        private fun createFileIfNotExist(path: Path) {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.createFile(path)
        }

        @Throws(IOException::class)
        private fun getDataPath(): String {
            val workingDirectory = File(System.getProperty("user.dir"))
            val path: Path = Paths.get("${workingDirectory.path}\\data")
            createDirectoryIfNotExist(path)
            return path.toString()
        }

        @Throws(IOException::class)
        fun selectOpenFile(stage: Stage?): File? {
            val fileChooser = FileChooser()
            fileChooser.initialDirectory = File(getDataPath())
            fileChooser.title = "Load Arena"
            fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Text File", "*.txt"))
            return fileChooser.showOpenDialog(stage)
        }

        @Throws(IOException::class)
        fun selectSaveFile(stage: Stage?): File? {
            val fileChooser = FileChooser()
            fileChooser.initialDirectory = File(getDataPath())
            fileChooser.title = "Save Arena"
            fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Text File", "*.txt"))
            return fileChooser.showSaveDialog(stage)
        }
    }
}