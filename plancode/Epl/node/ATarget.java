/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import java.util.*;
import Epl.analysis.*;

public final class ATarget extends PTarget
{
    private TTarg _targ_;
    private PBralist _bralist_;

    public ATarget()
    {
    }

    public ATarget(
        TTarg _targ_,
        PBralist _bralist_)
    {
        setTarg(_targ_);

        setBralist(_bralist_);

    }
    public Object clone()
    {
        return new ATarget(
            (TTarg) cloneNode(_targ_),
            (PBralist) cloneNode(_bralist_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseATarget(this);
    }

    public TTarg getTarg()
    {
        return _targ_;
    }

    public void setTarg(TTarg node)
    {
        if(_targ_ != null)
        {
            _targ_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _targ_ = node;
    }

    public PBralist getBralist()
    {
        return _bralist_;
    }

    public void setBralist(PBralist node)
    {
        if(_bralist_ != null)
        {
            _bralist_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _bralist_ = node;
    }

    public String toString()
    {
        return ""
            + toString(_targ_)
            + toString(_bralist_);
    }

    void removeChild(Node child)
    {
        if(_targ_ == child)
        {
            _targ_ = null;
            return;
        }

        if(_bralist_ == child)
        {
            _bralist_ = null;
            return;
        }

    }

    void replaceChild(Node oldChild, Node newChild)
    {
        if(_targ_ == oldChild)
        {
            setTarg((TTarg) newChild);
            return;
        }

        if(_bralist_ == oldChild)
        {
            setBralist((PBralist) newChild);
            return;
        }

    }
}