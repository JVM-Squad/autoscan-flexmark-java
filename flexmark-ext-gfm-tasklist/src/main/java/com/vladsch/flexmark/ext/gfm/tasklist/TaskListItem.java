package com.vladsch.flexmark.ext.gfm.tasklist;

import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.ListOptions;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/** A Task list item */
public class TaskListItem extends ListItem {
  private boolean isOrderedItem = false;
  private boolean canChangeMarker = true;

  @Override
  public void getAstExtra(StringBuilder out) {
    super.getAstExtra(out);
    if (isOrderedItem) out.append(" isOrderedItem");
    out.append(isItemDoneMarker() ? " isDone" : " isNotDone");
  }

  @Override
  public boolean isParagraphWrappingDisabled(
      Paragraph node, ListOptions listOptions, DataHolder options) {
    // see if this is the first paragraph child item we handle our own paragraph wrapping for that
    // one
    Node child = getFirstChild();
    while (child != null && !(child instanceof Paragraph)) child = child.getNext();
    return child == node;
  }

  public TaskListItem() {}

  public TaskListItem(ListItem block) {
    super(block);
    isOrderedItem = block instanceof OrderedListItem;
  }

  @Override
  public void setOpeningMarker(BasedSequence openingMarker) {
    throw new IllegalStateException();
  }

  public boolean isItemDoneMarker() {
    return !markerSuffix.matches("[ ]");
  }

  @Override
  public boolean isOrderedItem() {
    return isOrderedItem;
  }

  public void setOrderedItem(boolean orderedItem) {
    isOrderedItem = orderedItem;
  }

  @Override
  public boolean canChangeMarker() {
    return canChangeMarker;
  }

  public void setCanChangeMarker(boolean canChangeMarker) {
    this.canChangeMarker = canChangeMarker;
  }
}
