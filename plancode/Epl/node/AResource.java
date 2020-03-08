/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import java.util.*;
import Epl.analysis.*;

public final class AResource extends PResource
{
    private TRes _res_;
    private PAtom _atom_;

    public AResource()
    {
    }

    public AResource(
        TRes _res_,
        PAtom _atom_)
    {
        setRes(_res_);

        setAtom(_atom_);

    }
    public Object clone()
    {
        return new AResource(
            (TRes) cloneNode(_res_),
            (PAtom) cloneNode(_atom_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAResource(this);
    }

    public TRes getRes()
    {
        return _res_;
    }

    public void setRes(TRes node)
    {
        if(_res_ != null)
        {
            _res_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _res_ = node;
    }

    public PAtom getAtom()
    {
        return _atom_;
    }

    public void setAtom(PAtom node)
    {
        if(_atom_ != null)
        {
            _atom_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _atom_ = node;
    }

    public String toString()
    {
        return ""
            + toString(_res_)
            + toString(_atom_);
    }

    void removeChild(Node child)
    {
        if(_res_ == child)
        {
            _res_ = null;
            return;
        }

        if(_atom_ == child)
        {
            _atom_ = null;
            return;
        }

    }

    void replaceChild(Node oldChild, Node newChild)
    {
        if(_res_ == oldChild)
        {
            setRes((TRes) newChild);
            return;
        }

        if(_atom_ == oldChild)
        {
            setAtom((PAtom) newChild);
            return;
        }

    }
}
