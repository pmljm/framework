/*
 * Copyright 2007-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package util

import scala.util.parsing.combinator.Parsers

import org.specs2.mutable._
import org.specs2.matcher._
import org.specs2.ScalaCheck
import org.scalacheck.{Arbitrary, Gen, Prop}
import scala.collection.immutable._
import Gen._
import Prop._
object ParserHelpers extends CombParserHelpers with Parsers

object CombParserHelpersSpec extends Specification with ScalaCheck with ParserMatchers {
 "CombParserHelpers Specification".title
  val parsers = ParserHelpers
  /** bypass this implicit from the ParserMatchers trait */
  override def useStringsAsInput(s:String) = super.useStringsAsInput(s)
  def equalString[T](s: String) = new BeEqualTo(s) ^^ ((_:T).toString)
  import ParserHelpers._

  "The parser helpers" should {
    "provide an isEof function returning true iff a char is end of file" in {
      isEof('\032') must beTrue
    }
    "provide an notEof function returning true iff a char is not end of file" in {
      notEof('\032') must beFalse
    }
    "provide an isNum function returning true iff a char is a digit" in {
      isNum('0') must beTrue
    }
    "provide an notNum function returning true iff a char is not a digit" in {
      notNum('0') must beFalse
    }
    "provide an wsc function returning true iff a char is a space character" in {
      List(' ', '\t', '\r', '\n') foreach {wsc(_) must beTrue}
      wsc('a') must beFalse
    }
    "provide a whitespace parser: white. Alias: wsc" in checkProp { 
	    import WhiteStringGen._
	    (s: String) => wsc(s) must beASuccess
	  }
    "provide a whiteSpace parser always succeeding and discarding its result" in checkProp { 
	    import StringWithWhiteGen._
	    (s: String) => whiteSpace must succeedOn(s).withResult(equalString("()"))
    }
    "provide an acceptCI parser to parse whatever string matching another string ignoring case" in check { 
      implicit def abcd = Arbitrary { AbcdStringGen.abcdString }
	    (s: String, s2: String) => (acceptCI(s).apply(s2) must beASuccess.iff(s2.toUpperCase startsWith s.toUpperCase))
    }
    "provide a digit parser - returning a String" in check { (s: String) => 
      digit(s) must haveSuccessResult("\\p{Nd}")
    }
    "provide an aNumber parser - returning an Int if succeeding" in check { (s: String) =>
      aNumber(s) must haveSuccessResult("\\p{Nd}+")
    }
    "provide a slash parser" in {
      slash("/") must beASuccess.withResult(equalString("/"))
      slash("x") must beAFailure
    }
    "provide a colon parser" in {
      colon(":") must beASuccess.withResult(equalString(":"))
      colon("x") must beAFailure
    }
    "provide a EOL parser which parses the any and discards any end of line character" in {
      List("\n", "\r") foreach {
        s =>
          EOL(s) must beASuccess.withResult(equalString("()"))
          EOL(s).next.atEnd must beTrue
      }
	  success
    }
    val parserA = elem("a", (c: Char) => c == 'a')
    val parserB = elem("b", (c: Char) => c == 'b')
    val parserC = elem("c", (c: Char) => c == 'c')
    val parserD = elem("d", (c: Char) => c == 'd')
	
    "provide a permute parser succeeding if any permutation of given parsers succeeds" in checkProp { 
      implicit def abcd = Arbitrary { AbcdStringGen.abcdString }
	  (s: String) => permute(parserA, parserB, parserC, parserD) must succeedOn(s)
    }
	  
    val notEmpty = forAll(!new StringOps(_:String).isEmpty)
    "provide a permuteAll parser succeeding if any permutation of the list given parsers, or a sublist of the given parsers succeeds" in checkProp {
      implicit def pick3Letters = AbcdStringGen.pickN(3, List("a", "b", "c"))
      notEmpty ==> { (s:String) => permuteAll(parserA, parserB, parserC, parserD) must succeedOn(s) }
    }
    "provide a repNN parser succeeding if an input can be parsed n times with a parser" in checkProp {
      implicit def pick3Letters = AbcdStringGen.pickN(3, List("a", "a", "a"))
      notEmpty ==> { (s:String) => repNN(3, parserA) must succeedOn(s) }
    }
  }
}


object AbcdStringGen {
  implicit def abcdString =
    for (
      len <- choose(4, 4);
      string <- pick(len, List("a", "b", "c", "d"))
    ) yield string.mkString("")

  def pickN(n: Int, elems: List[String]) =
    Arbitrary { for (string <- pick(n, elems)) yield string.mkString("") }
}


object WhiteStringGen {
  def genWhite =
    for (
      len <- choose(1, 4);
      string <- listOfN(len, frequency((1, value(" ")), (1, value("\t")), (1, value("\r")), (1, value("\n"))))
    ) yield string.mkString("")

  implicit def genWhiteString: Arbitrary[String] = Arbitrary { genWhite }
}


object StringWithWhiteGen {
  import WhiteStringGen._

  def genStringWithWhite =
    for (
      len <- choose(1, 4);
      string <- listOfN(len, frequency((1, value("a")), (2, value("b")), (1, genWhite)))
    ) yield string.mkString("")

  implicit def genString: Arbitrary[String] = Arbitrary { genStringWithWhite }
}

