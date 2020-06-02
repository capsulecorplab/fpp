package fpp.compiler.codegen

import java.time.Year

/** Write a Cpp doc as hpp or cpp */
trait CppDocWriter extends CppDocVisitor with LineUtils {

  case class Input(
    /** The hpp file */
    hppFile: CppDoc.HppFile,
    /** The cpp file name */
    cppFileName: String,
    /** The list of enclosing class names, backwards. A class name may include :: */
    classNameList: List[String] = Nil,
  ) {

    def getEnclosingClassQualified = classNameList.reverse.mkString("::")
 
    def getEnclosingClassUnqualified = classNameList.head.split("::").reverse.head

  }

  def default(in: Input) = Nil

  /** Convert a param to a string with no trailing comma */
  def paramString(p: CppDoc.Function.Param): String

  /** Convert a param to a string with a trailing comma */
  def paramStringComma(p: CppDoc.Function.Param): String

  /** Visit a CppDoc */
  def visitCppDoc(cppDoc: CppDoc): Output

  /** Write a parameter with no trailing comma as a line */
  final def writeParam(p: CppDoc.Function.Param) = line(paramString(p))

  /** Write a parameter with a trailing comma as a line */
  final def writeParamComma(p: CppDoc.Function.Param) = line(paramStringComma(p))

  type Output = List[Line]

}

object CppDocWriter extends LineUtils {

  /** Write a banner comment */
  def writeBannerComment(comment: String) = {
    def banner =
      line("// ----------------------------------------------------------------------")
    (Line.blank :: banner :: writeCommentBody(comment)) :+ banner
  }

  /** Write a comment */
  def writeComment(comment: String) = Line.blank :: writeCommentBody(comment)

  /** Write an optional Doxygen comment */
  def writeDoxygenCommentOpt(commentOpt: Option[String]) = commentOpt match {
    case Some(comment) => writeDoxygenComment(comment)
    case None => Line.blank :: Nil
  }
    
  /** Write a Doxygen comment */
  def writeDoxygenComment(comment: String) = 
    Line.blank ::lines(comment).map(Line.join(" ")(line("//!"))_)
    
  /** Write a comment body */
  def writeCommentBody(comment: String) = lines(comment).map(Line.join(" ")(line("//"))_)

  /** Left align a compiler directive */
  def leftAlignDirective(line: Line) =
    if (line.string.startsWith("#")) Line(line.string) else line

  /** Write a header banner */
  def writeBanner(title: String) = lines(
    s"""|// ====================================================================== 
        |// \\title  $title
        |// \\author Generated by fpp-to-cpp
        |//
        |// \\copyright
        |// Copyright (C) ${Year.now.getValue} California Institute of Technology.
        |// ALL RIGHTS RESERVED.  United States Government Sponsorship
        |// acknowledged. Any commercial use must be negotiated with the Office
        |// of Technology Transfer at the California Institute of Technology.
        |// 
        |// This software may be subject to U.S. export control laws and
        |// regulations.  By accepting this document, the user agrees to comply
        |// with all U.S. export laws and regulations.  User has the
        |// responsibility to obtain export licenses, or other export authority
        |// as may be required before exporting such information to foreign
        |// countries or providing access to foreign persons.
        |// ======================================================================"""
  )

  /** Write a function body */
  def writeFunctionBody(body: List[Line]) = {
    val bodyLines = body.length match {
      case 0 => Line.blank :: Nil
      case _ => body.map(indentIn(_))
    }
    line("{") :: (bodyLines :+ line("}"))
  }

}
