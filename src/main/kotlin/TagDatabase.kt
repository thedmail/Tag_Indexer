import java.io.Serializable

class TagDatabase (
  narrativeTag:String,
  correspondingNarrativeFileList:MutableList<String>,
  associatedRefDocs:MutableList<String>
) :Serializable {
  val narrativeTag = narrativeTag
  val correspondingNarrativeFileList = correspondingNarrativeFileList
  var associatedRefDocs=associatedRefDocs
}

/* I removed these arguments and corresponding fields for the time being.
args:     file2Title:String,
    matchCount:Int,
    tagList:Set<String>

    fields:

        var file2Title = file2Title
    var matchCount = matchCount
    var tagList = tagList
 */