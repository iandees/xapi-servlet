package com.yellowbkpk.geo.xapi.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import com.yellowbkpk.geo.xapi.antlr.XAPILexer;
import com.yellowbkpk.geo.xapi.antlr.XAPIParser;
import com.yellowbkpk.geo.xapi.db.Selector;

public class XAPIQueryInfo {
	
	public enum RequestType {
		ALL("*"),
		NODE("node"),
		WAY("way"),
		RELATION("relation"),
		MAP("map?");
		
		private static Map<String, RequestType> val = new HashMap<String, RequestType>();
		static {
			for (RequestType i : values()) {
				val.put(i.t, i);
			}
		}
		private String t;
		private RequestType(String t) {
			this.t = t;
		}
		public static RequestType fromValue(String v) {
			return val.get(v);
		}
	}

	private RequestType type;
	private List<Selector.BoundingBox> boundingBoxes;
	private List<Selector> selectors;

	private XAPIQueryInfo(RequestType type, List<Selector> selectors, List<Selector.BoundingBox> bboxSelectors) {
		this.type = type;
		this.selectors = selectors;
		this.boundingBoxes = bboxSelectors;
	}
	
	public static XAPIQueryInfo fromString(String str) throws XAPIParseException {
		try {
			CharStream stream = new ANTLRStringStream(str);
			XAPILexer lexer = new XAPILexer(stream);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			XAPIParser parser = new XAPIParser(tokens);
			return fromAST((CommonTree) parser.xapi().getTree());
		} catch (RecognitionException e) {
			throw new XAPIParseException(e);
		}
	}

	private static XAPIQueryInfo fromAST(Tree tree) {
		Map<String, List<Tree>> map = childNodes(tree);
		
		RequestType type = RequestType.fromValue(valueOf(map.get("REQUEST_KIND")));
		
		List<Selector> selectors = new LinkedList<Selector>();
		List<Selector.BoundingBox> bboxSelectors = new LinkedList<Selector.BoundingBox>();
		List<Tree> predicateTrees = map.get("BBOX_PREDICATE");
		if(predicateTrees != null) {
			for (Tree predicateTree : predicateTrees) {
				bboxSelectors.add(buildBboxSelector(predicateTree));
			}
		}
		
		predicateTrees = map.get("TAG_PREDICATE");
		if(predicateTrees != null) {
			for (Tree predicateTree : predicateTrees) {
				Selector[] tags = buildTagSelector(predicateTree);
				for (Selector selector : tags) {
					selectors.add(selector);
				}
			}
		}
		
		return new XAPIQueryInfo(type, selectors, bboxSelectors);
	}

	private static Selector[] buildTagSelector(Tree predicateTree) {
		Map<String, List<Tree>> children = childNodes(predicateTree);
		Tree leftTree = children.get("LEFT").get(0);
		Tree rightTree = children.get("RIGHT").get(0);
		
		String[] leftValues = childTreeText(leftTree);
		String[] rightValues = childTreeText(rightTree);
		
		// Wildcard on the right
		if(rightValues.length == 1 && rightValues[0].equals("*")) {
			Selector.Tag.Wildcard[] retVal = new Selector.Tag.Wildcard[leftValues.length];
			int i = 0;
			for (String value : leftValues) {
				retVal[i++] = new Selector.Tag.Wildcard(value);
			}
			return retVal;
		}
		
		Selector[] selectors = new Selector[leftValues.length * rightValues.length];
		int i = 0;
		for (String left : leftValues) {
			for (String right : rightValues) {
				selectors[i++] = new Selector.Tag(left, right);
			}
		}
		return selectors;
	}

	private static String[] childTreeText(Tree tree) {
		String[] ret = new String[tree.getChildCount()];
		for (int i = 0; i < tree.getChildCount(); i++) {
			ret[i] = tree.getChild(i).getText();
		}
		return ret;
	}

	private static Selector.BoundingBox buildBboxSelector(Tree predicateTree) {
		double left = Double.parseDouble(predicateTree.getChild(0).getText());
		double bottom = Double.parseDouble(predicateTree.getChild(1).getText());
		double right = Double.parseDouble(predicateTree.getChild(2).getText());
		double top = Double.parseDouble(predicateTree.getChild(3).getText());
		return new Selector.BoundingBox(left, right, top, bottom);
	}

	private static String valueOf(List<Tree> list) {
		return list.get(0).getChild(0).getText();
	}

	private static Map<String, List<Tree>> childNodes(Tree tree) {
		Map<String, List<Tree>> ret = new HashMap<String, List<Tree>>();
		for(int i = 0; i < tree.getChildCount(); i++) {
			Tree child = tree.getChild(i);
			List<Tree> children = ret.get(child.getText());
			if(children == null) {
				children = new LinkedList<Tree>();
				ret.put(child.getText(), children);
			}
			children.add(child);
		}
		return ret;
	}

	public RequestType getKind() {
		return type;
	}

	public List<Selector> getTagSelectors() {
		return selectors;
	}

	public List<Selector.BoundingBox> getBboxSelectors() {
		return boundingBoxes;
	}

}
