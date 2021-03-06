/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import java.util.*;
import Epl.analysis.*;

public final class ATechstatStatement extends PStatement
{
    private PTechnique _technique_;

    public ATechstatStatement()
    {
    }

    public ATechstatStatement(
        PTechnique _technique_)
    {
        setTechnique(_technique_);

    }
    public Object clone()
    {
        return new ATechstatStatement(
            (PTechnique) cloneNode(_technique_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseATechstatStatement(this);
    }

    public PTechnique getTechnique()
    {
        return _technique_;
    }

    public void setTechnique(PTechnique node)
    {
        if(_technique_ != null)
        {
            _technique_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _technique_ = node;
    }

    public String toString()
    {
        return ""
            + toString(_technique_);
    }

    void removeChild(Node child)
    {
        if(_technique_ == child)
        {
            _technique_ = null;
            return;
        }

    }

    void replaceChild(Node oldChild, Node newChild)
    {
        if(_technique_ == oldChild)
        {
            setTechnique((PTechnique) newChild);
            return;
        }

    }
}
