package mill.discover

import mill.define.{Segment, Target, Task}
import ammonite.main.Router.EntryPoint

import scala.language.experimental.macros

/**
  * Metadata about a build that is extracted & materialized at compile-time,
  * letting us use it at run-time without needing to use the heavy weight
  * scala-reflect library.
  *
  * Note that [[Mirror]] allows you to store and inspect metadata of a type
  * [[T]] even without a concrete instance of [[T]] itself.
  */
case class Mirror[-T, V](node: (T, List[List[Any]]) => V,
                         commands: Seq[EntryPoint[V]],
                         targets: Seq[Mirror.TargetPoint[V, _]],
                         children: List[(String, Mirror[T, _])],
                         crossChildren: Option[(V => List[List[Any]], Mirror[T, _])])

object Mirror{
  def renderSelector(selector: Seq[Segment]) = selector.toList match {
    case Nil => ""
    case Segment.Label(head) :: rest =>
      val stringSegments = rest.map{
        case Segment.Label(s) => "." + s
        case Segment.Cross(vs) => "[" + vs.mkString(",") + "]"
      }
      head + stringSegments.mkString
  }


  def traverse[T, V, R](t: T, hierarchy: Mirror[T, V])
                       (f: (Mirror[T, _], => Seq[Segment]) => Seq[R]): Seq[R] = {
    def rec[C](segmentsRev: List[Segment], h: Mirror[T, C]): Seq[R]= {
      val crossValues = segmentsRev.map{case Segment.Cross(vs) => vs case _ => Nil}
      val self = f(h, segmentsRev)
      self ++
      h.children.flatMap{case (label, c) => rec(Segment.Label(label) :: segmentsRev, c)} ++
      h.crossChildren.toSeq.flatMap{
        case (crossGen, c) =>
          crossGen(h.node(t, crossValues.reverse.map(_.toList))).flatMap(cross =>
            rec(Segment.Cross(cross) :: segmentsRev, c)
          )
      }
    }
    rec(Nil, hierarchy)
  }


  /**
    * Represents metadata about a particular target, before the target is
    * materialized for a concrete build
    */
  case class TargetPoint[T, V](label: String, run: T => Target[V])
}
