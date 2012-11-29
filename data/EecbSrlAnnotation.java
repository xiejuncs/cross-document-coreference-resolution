package edu.oregonstate.data;

/**
 * Implementation for aligning the result of SRL and the gold annotations
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbSrlAnnotation {
	
	/** The id. */
	String id;
	
	/** the text */
	String mText;
	
	/** The start offset */
	int start;

	/** The end offset */
	int end;
	
	/** head dependency */
	int parentPosition;
	
	/*mention headWord*/
	String headString;
	
	/**predicate*/
	String predicate;

	public EecbSrlAnnotation(String id, String text, String predicate, int parentPosition, int start, int end) {
	  this.id = id;
	  this.mText = text;
	  this.predicate = predicate;
	  this.parentPosition = parentPosition;
	  this.start = start;
	  this.end = end;
	}
	
	public EecbSrlAnnotation() {
		
	}
	
	public void setHead(String headString) {
		this.headString = headString;
	}
	
	public String getHead() {
		return this.headString;
	}
	
	/** The ID of the annotation. */
	public String getId()
	{
	  return id;
	} // getId()

	/** Set the ID of the annotation. */
	public void setId(String i)
	{
	  id = i;
	} // setId()

	public String getText() {
		return this.mText;
	}
	
	public String getPredicate() {
		return this.predicate;
	}

	/** The start offset. */
	public int getStartOffset()
	{
	  return start;
	} // getStartOffset()

	/** The end offset. */
	public int getEndOffset()
	{
	  return end;
	} // getEndOffset()

	public int getLength()
	{
	  return end - start;
	}

	/** Set the start offset. */
	public void setStartOffset(int s)
	{
	  start = s;
	} // setStartOffset()

	/** Set the end offset. */
	public void setEndOffset(int e)
	{
	  end = e;
	} // setEndOffset()

	/** Output representation of the annotation */
	@Override
	public String toString() {
		return mText + "(" + start + " " + end + ")";
	}
	
	public void setText(String text) {
		mText = text;
	}
	
	
}
