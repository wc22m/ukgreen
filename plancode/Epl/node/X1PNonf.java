/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import Epl.analysis.*;

public final class X1PNonf extends XPNonf
{
    private XPNonf _xPNonf_;
    private PNonf _pNonf_;

    public X1PNonf()
    {
    }

    public X1PNonf(
        XPNonf _xPNonf_,
        PNonf _pNonf_)
    {
        setXPNonf(_xPNonf_);
        setPNonf(_pNonf_);
    }

    public Object clone()
    {
        throw new RuntimeException("Unsupported Operation");
    }

    public void apply(Switch sw)
    {
        throw new RuntimeException("Switch not supported.");
    }

    public XPNonf getXPNonf()
    {
        return _xPNonf_;
    }

    public void setXPNonf(XPNonf node)
    {
        if(_xPNonf_ != null)
        {
            _xPNonf_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _xPNonf_ = node;
    }

    public PNonf getPNonf()
    {
        return _pNonf_;
    }

    public void setPNonf(PNonf node)
    {
        if(_pNonf_ != null)
        {
            _pNonf_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _pNonf_ = node;
    }

    void removeChild(Node child)
    {
        if(_xPNonf_ == child)
        {
            _xPNonf_ = null;
        }

        if(_pNonf_ == child)
        {
            _pNonf_ = null;
        }
    }

    void replaceChild(Node oldChild, Node newChild)
    {
    }

    public String toString()
    {
        return "" +
            toString(_xPNonf_) +
            toString(_pNonf_);
    }
}