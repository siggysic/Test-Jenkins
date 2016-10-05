package com.acme.pizza

import org.scalatest._
import selenium._
import org.openqa.selenium.chrome

class PizzaSpecs extends FlatSpec with Chrome with Matchers {
  // implicit val webDriver: WebDriver = new FirefoxDriver

  val host = "http://localhost/services-my-self/"

  "The blog app home page" should "have the correct title" in {
    go to (host)
    textField("test_text").value = "Test Jar"
    textField("test_text").value should be ("Test Jar")
    textArea("test_area").value = "I saw something cool today!"
    textArea("test_area").value should be ("I saw something cool today!")
    click on name ("test_but")
    Thread.sleep(2000)
    val a = switch to alertBox
    a.getText() should be ("Qwe")
    a.accept()
    quit()
  }
}
