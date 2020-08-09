/*
 *    CaseFolder.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package edu.kit.aifb.gwifi.util.text;

import java.io.File;

import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;

/**
 * @author dnk2
 *
 *	A text processor which simply turns all characters into lower case.
 */
public class CaseFolder extends TextProcessor {

	public String processText(String text) {
		return text.toLowerCase() ;
	}

	public static void main(String args[]) throws Exception {

		CaseFolder folder = new CaseFolder() ;

	    WikipediaConfiguration conf = new WikipediaConfiguration(new File(args[0])) ;
	    WEnvironment.prepareTextProcessor(folder, conf, new File("tmp"), true, 1) ;
	}
	
}
