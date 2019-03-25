package root.data

import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import java.io.File

data class SquadSticker(val useImageId: Boolean, val fileId: String, val path: String) {
    fun getSticker() = SendSticker().apply {
        if(useImageId)
            setSticker(fileId)
        else
            setSticker(File(path))
    }
}