package root.data.entity

abstract class ExcelEntity {
    abstract fun toRow():Array<String>
    abstract fun toHead():Array<String>
}