package root.data.entity

interface ExcelEntity {
    fun toRow():Array<String>
    fun toHead():Array<String>
}