package ai.common;

import java.io.Serializable;


public class NodeDistance implements Comparable<NodeDistance>, Serializable {

	private static final long serialVersionUID = -7074207573826031573L;
	public Node node;
	public int f, g, h;
	public NodeDistance parent;

	public NodeDistance(Node node, int g, int h, NodeDistance parent) {
		this.node = node;
		this.g = g;
		this.h = h;
		f = g + h;
		this.parent = parent;
	}

	@Override
	public int compareTo(NodeDistance that) {
		return this.f - that.f;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + f;
		result = prime * result + g;
		result = prime * result + h;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeDistance other = (NodeDistance) obj;
		if (f != other.f)
			return false;
		if (g != other.g)
			return false;
		if (h != other.h)
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}



}