package root.data.entity

abstract class EntityData {
    abstract fun toRow():Array<String>
}