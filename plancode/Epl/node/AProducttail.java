/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import java.util.*;
import Epl.analysis.*;

public final class AProducttail extends PProducttail
{
    private PAtom _atom_;

    public AProducttail()
    {
    }

    public AProducttail(
        PAtom _atom_)
    {
        setAtom(_atom_);

    }
    public Object clone()
    {
        return new AProducttail(
            (PAtom) cloneNode(_atom_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAProducttail(this);
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
            + toString(_atom_);
    }

    void removeChild(Node child)
    {
        if(_atom_ == child)
        {
            _atom_ = null;
            return;
        }

    }

    void replaceChild(Node oldChild, Node newChild)
    {
        if(_atom_ == oldChild)
        {
            setAtom((PAtom) newChild);
            return;
        }

    }
}
