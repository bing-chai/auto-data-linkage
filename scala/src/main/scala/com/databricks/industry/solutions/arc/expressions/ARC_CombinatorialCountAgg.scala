package com.databricks.industry.solutions.arc.expressions

import com.databricks.industry.solutions.arc.expressions.base.{CountAccumulatorMap, Utils}
import org.apache.commons.lang3.SerializationUtils
import org.apache.spark.sql.catalyst.expressions.aggregate.{ImperativeAggregate, TypedImperativeAggregate}
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._

case class ARC_CombinatorialCountAgg(
    attributeExprs: Seq[Expression],
    attributeNames: Seq[String],
    nCombination: Int = 2,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0
) extends TypedImperativeAggregate[CountAccumulatorMap] {

    private val attributeMap = attributeNames.zip(attributeExprs).toMap
    private val combinations = attributeMap.values.toList.combinations(nCombination).toList

    override def children: Seq[Expression] = attributeMap.values.toSeq

    override def createAggregationBuffer(): CountAccumulatorMap = CountAccumulatorMap()

    override def update(buffer: CountAccumulatorMap, input: InternalRow): CountAccumulatorMap = {
        val left = combinations.map(
          _.map(_.eval(input))
              .mkString("", ",", "")
        )
        buffer.merge(CountAccumulatorMap(left))
    }

    override def merge(buffer: CountAccumulatorMap, input: CountAccumulatorMap): CountAccumulatorMap = {
        buffer.merge(input)
    }

    override def eval(buffer: CountAccumulatorMap): Any = {
        val result = Utils.buildMapLong(buffer.counter)
        result
    }

    override def serialize(buffer: CountAccumulatorMap): Array[Byte] = {
        SerializationUtils.serialize(buffer.asInstanceOf[Serializable])
    }

    override def deserialize(storageFormat: Array[Byte]): CountAccumulatorMap = {
        SerializationUtils.deserialize(storageFormat).asInstanceOf[CountAccumulatorMap]
    }

    override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
        copy(mutableAggBufferOffset = newMutableAggBufferOffset)

    override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
        copy(inputAggBufferOffset = newInputAggBufferOffset)

    override def nullable: Boolean = false

    override def dataType: DataType = MapType(StringType, LongType)

    override protected def withNewChildrenInternal(newChildren: IndexedSeq[Expression]): Expression = copy(attributeExprs = newChildren)

}
