package mill.define

case class Cross[+T](items: List[(List[Any], T)])(implicit val e: sourcecode.Enclosing, val l: sourcecode.Line){
  def flatMap[V](f: T => Cross[V]): Cross[V] = new Cross(
    items.flatMap{
      case (l, v) => f(v).items.map{case (l2, v2) => (l2 ::: l, v2)}
    }
  )
  def map[V](f: T => V): Cross[V] = new Cross(items.map{case (l, v) => (l, f(v))})
  def withFilter(f: T => Boolean): Cross[T] = new Cross(items.filter(t => f(t._2)))

  def applyOpt(input: Any*): Option[T] = {
    val inputList = input.toList
    items.find(_._1 == inputList).map(_._2)
  }
  def apply(input: Any*): T = {
    applyOpt(input:_*).getOrElse(
      throw new Exception(
        "Unknown set of cross values: " + input +
        " not in known values\n" + items.map(_._1).mkString("\n")
      )
    )
  }
}
object Cross{
  def apply[T](t: T*) = new Cross(t.map(i => List(i) -> i).toList)
}

class CrossModule[T, V](constructor: (T, Module.Ctx) => V, cases: T*)
                       (implicit ctx: Module.Ctx)
extends Cross[V]({
  cases.toList.map(x =>
    (
      List(x),
      constructor(
        x,
        ctx.copy(
          segments0 = Segments(ctx.segments0.value :+ ctx.segment),
          segment = Segment.Cross(List(x))
        )
      )
    )
  )
})

class CrossModule2[T1, T2, V](constructor: (T1, T2, Module.Ctx) => V, cases: (T1, T2)*)
                             (implicit ctx: Module.Ctx)
extends Cross[V](
  cases.toList.map(x =>
    (
      List(x._2, x._1),
      constructor(
        x._1, x._2,
        ctx.copy(
          segments0 = Segments(ctx.segments0.value :+ ctx.segment),
          segment = Segment.Cross(List(x._2, x._1))
        )
      )
    )
  )
)

class CrossModule3[T1, T2, T3, V](constructor: (T1, T2, T3, Module.Ctx) => V, cases: (T1, T2, T3)*)
                                 (implicit ctx: Module.Ctx)
extends Cross[V](
  cases.toList.map(x =>
    (
      List(x._3, x._2, x._1),
      constructor(
        x._1, x._2, x._3,
        ctx.copy(
          segments0 = Segments(ctx.segments0.value :+ ctx.segment),
          segment = Segment.Cross(List(x._3, x._2, x._1))
        )
      )
    )
  )
)