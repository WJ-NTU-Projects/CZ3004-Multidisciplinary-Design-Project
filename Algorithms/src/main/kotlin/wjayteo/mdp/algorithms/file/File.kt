package wjayteo.mdp.algorithms.file

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.*


class File {
    companion object {
        const val CONNECTION = "connection.txt"

        @Throws(IOException::class)
        fun read(file: String): List<String> {
            val path: Path = Paths.get("data/$file")
            createDirectoryIfNotExist(Paths.get("data"))
            createFileIfNotExist(path)
            return Files.readAllLines(path)
        }

        @Throws(IOException::class)
        fun replaceContent(file: String, text: String) {
            val path: Path = Paths.get("data/$file")
            createDirectoryIfNotExist(Paths.get("data"))
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
    }
}