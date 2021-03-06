package net.orworks.mbdeployer

import java.io._
import java.nio.file._

import scala.io.Source
import scala.collection.mutable

object Main extends App
{
    if(args.length == 0)
        println(s"Usage: scala mbdeployer-<version>.jar [/path/to/source/folder] [/path/to/destination/folder] [-all]")

    /**
      * Extension class for arrays
      * @param array
      * @tparam A
      */
    implicit class ArrayUtils[A](array: Array[A])
    {
        /**
          * Method to check if an array doesn't contain an element.
          * @param elem
          * @tparam A
          * @return
          */
        def doesntContain[A](elem: A) = !array.contains(elem)
    }

    val sourceBase = new File(if(args.length == 0) "/home/samik/git/myoptcloud-web" else args(0))
    println(s"** Using source base: [${sourceBase.getPath}]")
    val destinationBase = new File(if(args.length == 0) "/home/samik/firebase/public" else args(1)).getCanonicalPath
    println(s"** Using destination base: [$destinationBase]")

    // Read the default skiplists from config file
    val configJson = Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/config.json")).getLines().mkString(" "))
    val fileSkipList = configJson("fileSkipList").asArray.map(_.asString)
    val folderSkipList = configJson("folderSkipList").asArray.map(_.asString)

    // Get the JSON file containing global code changes
    val globalJSONFile = new File(s"${sourceBase.getCanonicalPath}/global.json")
    val (globalFileSkipList, globalEdits) = if(globalJSONFile.exists())
        {
            val fileJson = Json.parse(getFileAsString(globalJSONFile))
            (fileJson.asMap("globalFileSkipList").asArray.map(_.toString), fileJson.asMap("items").asArray.map(_.asMap))
        }
    else (Array[String](), Array[Map[String,Json.Value]]())
    if(globalEdits.length > 0) println(s"** Using global enhancement list from [${globalJSONFile.getPath}], count of global enhancements: ${globalEdits.length}")

    // Collect the list of all files that needs to be processed
    val allFiles = getRecursiveListOfFiles(sourceBase)

    // Load saved information about last timestamp of each of the files
    val savedTSPath = s"${System.getProperty("java.io.tmpdir")}/lastModDates.tmp"
    val savedFileTS = new File(savedTSPath)
    val lastModDates = mutable.HashMap[String, Long]() ++= (if(savedFileTS.exists()) (Json.parse(this.getFileAsString(savedFileTS))).asMap.map(tp => tp._1 -> tp._2.asLong) else mutable.HashMap[String, Long]())
    if(lastModDates.size > 0) println(s"** Using last modified timestamps loaded from [$savedTSPath], # of entries loaded: ${lastModDates.size}. To skip timestamp comparison, use '-all' at the command line.")

    // Now start copying
    allFiles.foreach(entry =>
    {
        // Construct the destination paths
        val destPath = Paths.get(destinationBase, entry.toString.replaceFirst(sourceBase.getPath, ""))

        if(entry.isDirectory)
            new File(destPath.toString).mkdirs()
        else if(args.doesntContain("-all") && entry.lastModified() == lastModDates.getOrElse(entry.getAbsolutePath, 0))
            println(s"Skipping [${entry.toPath}]: File unchanged.")
        else
        {
            println(s"Processing: [${entry.toPath}]")
            // These steps are only for HTML files
            if(destPath.toString.endsWith(".html"))
            {
                // Read the source file by lines
                val sourceFile = Source.fromFile(entry).getLines().toBuffer
                // Look for local edits, specific to this file
                val editFile = new File(entry.toString.replace(".html", ".json"))
                val fileEdits = if(editFile.exists()) Json.parse(getFileAsString(editFile)).asMap("items").asArray.map(_.asMap) else Array[Map[String,Json.Value]]()

                // Now combine global edits and local edits, and apply them in order
                // Check that this file is not in the global fileskiplist.
                val editList = (if(globalFileSkipList.doesntContain(destPath.getFileName().toString)) globalEdits else Array[Map[String,Json.Value]]()).union(fileEdits)
                editList.foreach(edit =>
                    edit("action").asString match
                    {
                        case "insert" =>
                            val lineNum = edit("line").asInt
                            sourceFile.insertAll(lineNum - 1, edit("code").asArray.map(_.asString))
                            println(s" Applied [${edit("desc").asString}/insert] at file: [${entry.toString}]")

                        case "delete" =>
                            val lines = edit("lines").asArray.map(_.asInt)
                            assert(lines.length <= 2, "Format: \"lines\": [lineNum] OR \"lines\": [First lineNum, Last lineNum]")
                            sourceFile.remove(lines(0) - 1, if(lines.length > 1) (lines(1) - lines(0) + 1) else 1)
                            println(s" Applied [${edit("desc").asString}/delete] at file: [${entry.toString}]")

                        case "replace" =>
                            val lines = edit("lines").asArray.map(_.asInt)
                            assert(lines.length <= 2, "Format: \"lines\": [lineNum] OR \"lines\": [First lineNum, Last lineNum]")
                            sourceFile.remove(lines(0) - 1, if(lines.length > 1) (lines(1) - lines(0) + 1) else 1)
                            sourceFile.insertAll(lines(0) - 1, edit("code").asArray.map(_.asString))
                            println(s" Applied [${edit("desc").asString}/replace] at file: [${entry.toString}]")

                        case x => throw new UnsupportedOperationException(s"Unsupported action type: $x")
                    }
                )

                // Finally, write the file to the destination
                Files.write(destPath, sourceFile.mkString("\n").getBytes)
            }
            else
                Files.copy(entry.toPath, destPath, StandardCopyOption.REPLACE_EXISTING)

            // Update last modification date
            lastModDates(entry.getAbsolutePath) = entry.lastModified()
            println(s"Copied ${entry.toPath} to $destPath")
        }
    })
    // Finally, save the last modified file dates in a file.
    Files.write(Paths.get(savedTSPath), new Json.Value(lastModDates.toMap).writeln.getBytes)
    println(s"** Last modified timestamps saved at: [$savedTSPath], # of entries saved: ${lastModDates.size}")

    /**
      * Get a recursive listing of all files underneath the given directory.
      * from stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
      */
    def getRecursiveListOfFiles(dir: File): Array[File] =
    {
        val files = Option(dir.listFiles)
        // Prepare a list of files that would be considered
        // Filter out json files, other files and foldes from skiplists
        val these = files.getOrElse(Array())
            .filterNot(fn => fn.getPath.endsWith(".json") || fileSkipList.contains(fn.getName) || folderSkipList.contains(fn.getName))
        // Now recursively visit subfolders.
        these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
    }

    /**
      * Method to get the contents of a file as String.
      * @param file
      * @return
      */
    def getFileAsString(file: File) = Source.fromFile(file).getLines().mkString(" ")
}
