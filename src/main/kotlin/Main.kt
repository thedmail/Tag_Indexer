import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Scanner
import kotlin.system.exitProcess
import java.io.ObjectOutputStream
import java.io.ObjectInputStream

fun main(args: Array<String>) {
  val scanner = Scanner(System.`in`)

  val narrDirectory = "/google/src/cloud/dmail/compose-indexxer/google3/third_party/devsite/android/en/jetpack/compose"
  val refDirectory = "/google/src/cloud/dmail/compose-recs-newmd/google3/third_party/devsite/android/en/reference/kotlin/androidx/compose"
  val mdFiles = getNarrativeFileList(narrDirectory)
  val refDocs= getRefDocList(refDirectory)
  val tagIndex=TagExtractor(mdFiles,narrDirectory)
  val navMap=NavMapper(narrDirectory,refDirectory)
  val fullMap=refCheck(navMap,narrDirectory,refDirectory)
  // Injector(fullMap)
  println()


//   TagExtractor(listOfFiles)
  //   val records= matchAndWrite(listOfFiles)
  //   val sortedRecords=sortRecords(records)
  //   val culledRecords=cullRecords(sortedRecords)
//    val completedRecords=getPageNames(culledRecords)
  //   removeExistingRecBlock(listOfFiles)
//    injectRecs(workingDirectory,destinationDirectory,completedRecords)
  println("...and done.")
}

//////////////////////////////////////////////////////////////
// Collects a list of narrative files.
fun getNarrativeFileList(directory:String): MutableList<String> {
  val baseDir = File(directory)
  val listOfFiles= mutableListOf<String>()

  // Get filetree
  val fileTree = baseDir.walkTopDown()

  // Iterate through the filetree and store the **narrative** files in the tag database
  for (file in fileTree) {
    if (!file.name.endsWith(".md")) continue
    if (file.name.startsWith("_")) continue
    listOfFiles.add((file.canonicalPath))
  }
  return listOfFiles
}

//////////////////////////////////////////////////////////////
// Collects a list of reference files
fun getRefDocList(directory:String): MutableList<String> {
  val baseDir = File(directory)
  val listOfFiles= mutableListOf<String>()

  // Get filetree
  val fileTree = baseDir.walkTopDown()

  // Iterate through the filetree and store the **API-reference** files in the tag database
  for (file in fileTree) {
    if (!file.name.endsWith(".html")) continue
    if (file.name.startsWith("_")) continue
    listOfFiles.add((file.canonicalPath))
  }
  return listOfFiles
}

/////////////////////////////////////////////
fun TagExtractor (listOfNarrativeFiles: MutableList<String>,narrDirectory:String)
{
  val indexFilePath=(narrDirectory+"/mdFiles.idx")
  val indexWriter=FileWriter(indexFilePath)

  // Iterate through the filetree
  for (file in listOfNarrativeFiles) {
    val finalResult = mutableListOf<String>()
    val result = mutableListOf<Char>()
    var counter = 0

    // Read the content of each file into a big String
    val testFileContent = File(file).readText()

    // See if the next three characters are consecutive backticks. Start
    // by reading them into three variables.
    for (i in 0..testFileContent.length - 3) {
      var currentChar = testFileContent[i]
      var nextChar = testFileContent[i + 1]
      var nextNextChar = testFileContent[i + 2]

      // Skip over code blocks (denoted by three backticks)
      // This only skips the first backtick, but then the subsequent two
      // end up being a pair containing nothing. The subsequent if statement
      // skips over pairs of backticks with nothing between them.
      if (currentChar == '`' && nextChar == '`' && nextNextChar == '`')
        continue

      if (currentChar == '`') {
        counter++
        continue
      }
      if (counter == 1 && currentChar == '`') {
        counter=0
        continue
      }
      if ((counter==1) && ((currentChar.isLetter()) || currentChar=='.'))
        result.add(testFileContent[i])
      else {
        result.add(' ')
        counter = 0
        continue
      }
    }
    val resultAsString = StringBuilder()
    for (i in result)
      resultAsString.append(i)
    val finalString = resultAsString.toString()
    val finalStringSplit = finalString.split(" ")

    val uniqueWords=finalStringSplit.toSet()

    for (word in uniqueWords)
      if (word.isNotEmpty())
        finalResult.add(word)
    finalResult.sort()
    val outputFileName=file+"-tag"
    val tagListWriter=FileWriter(outputFileName)

    for (word in finalResult)
    {

      tagListWriter.write(word+"\n")
      indexWriter.append(word+"\n")

    }
    tagListWriter.close()
  }
  indexWriter.close()


  val indexContent=File(indexFilePath).readLines()
  val writeSortedUniqueIndex=FileWriter(indexFilePath)
  val uniqueIndex=indexContent.toSet()
  val sortedIndexUnique=uniqueIndex.sorted()
  for (word in sortedIndexUnique)
    writeSortedUniqueIndex.write(word+"\n")
  writeSortedUniqueIndex.close()


}

/////////////////////////////////////////
fun NavMapper (narrDirectory: String,refDirectory:String):MutableList<TagDatabase>
{
  val mdFilesDir=File(narrDirectory)
  val refFilesDir=File(refDirectory+"/ref_files")
  val indexFile=File(mdFilesDir.canonicalPath+"/mdFiles.idx")
  val mdFileList=mdFilesDir.walkTopDown()
  val tagFiles=mutableListOf<String>()
  val correspondingNarrativeFileList= mutableListOf<String>()
  val theMap= mutableListOf<TagDatabase>()

// Read the overall list of tags from the index file into a String array
  val indexContents=indexFile.readLines()

// Now, declare the list of tagged files, based on the filetree constructed above.
  for (file in mdFileList)
    if (file.canonicalPath.endsWith("-tag"))
      tagFiles.add((file.canonicalPath))

// For each word...
  for (word in indexContents) {
    val record=TagDatabase(word, mutableListOf<String>(), mutableListOf<String>())
    // Check and see if it occurs in each tagged file. So, first let's read the list of tags
    // from each tagged file that we're on:
    for (tagFile in tagFiles) {
      val wordSearch=File(tagFile).readLines()
      if (wordSearch.contains(word)) {
        record.correspondingNarrativeFileList.add(tagFile)
      }
    }
    theMap.add(record)
  }

  val exportIndexToTextFile=FileWriter(narrDirectory+"/mdIndex.csv")
  for (record in theMap) {
    exportIndexToTextFile.write((record.narrativeTag + "\t"))
    var counter=0
    for (word in record.correspondingNarrativeFileList) {
      counter++
      if (counter==1)
        exportIndexToTextFile.write(word + "\n")
      else
        exportIndexToTextFile.write( "\t"+word+"\n")
    }
    exportIndexToTextFile.write("\n")
  }

  exportIndexToTextFile.close()
  return theMap
}

fun refCheck (navMap:MutableList<TagDatabase>,
              narrDirectory: String,refDirectory: String):MutableList<TagDatabase> {
  val fullMapIndex = File(narrDirectory + "/fullBidiIndex.idx")
  val refDocFileList = File(refDirectory).walkTopDown()
  for (record in navMap) {
    for (filename in refDocFileList) {
      if (record.narrativeTag == filename.nameWithoutExtension)
        record.associatedRefDocs.add(filename.canonicalPath)
    }
  }
  val objectOutputStream = ObjectOutputStream(fullMapIndex.outputStream())
  for (record in navMap) {
    objectOutputStream.writeObject(record)
  }
    objectOutputStream.close()
  return navMap
}

fun Injector (fullMap:MutableList<TagDatabase>) {
  for (record in fullMap) {
    if (record.associatedRefDocs.size==0)
      continue
    // Inject introductory line somewhere in toward top of ref-docs file, and a \n
    //Iow: println("For information about this class or method in context, see:\n")
    // Then, iterate through the matching files (i.e., correspondingNarrativeFileList)
    // and write each one to the ref-docs file under the introductory line
  }
}

/*
fun printHelp() {
    println("TODO: manpage")
    exitProcess(0)
}


TODO:
Invite reader to edit index files before continuing

 */