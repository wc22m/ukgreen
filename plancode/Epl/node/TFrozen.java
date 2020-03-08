/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import Epl.analysis.*;

public final class TFrozen extends Token
{
    public TFrozen()
    {
        super.setText("Frozen");
    }

    public TFrozen(int line, int pos)
    {
        super.setText("Frozen");
        setLine(line);
        setPos(pos);
    }

    public Object clone()
    {
      return new TFrozen(getLine(), getPos());
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTFrozen(this);
    }

    public void setText(String text)
    {
        throw new RuntimeException("Cannot change TFrozen text.");
    }
}
