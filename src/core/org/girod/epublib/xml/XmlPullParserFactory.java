/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.girod.epublib.xml;

/**
 *
 * @author Herve
 */
public class XmlPullParserFactory {
   private static XmlPullParserFactory factory = null;

   private XmlPullParserFactory() {
   }

   public static XmlPullParserFactory getInstance() {
      if (factory == null) {
         factory = new XmlPullParserFactory();
      }
      return factory;
   }

   public XmlSerializer newSerializer() {
      XmlSerializer serializer = new XmlSerializerImpl();
      return serializer;
   }
}
