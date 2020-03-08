/* This file was generated by SableCC (http://www.sablecc.org/). */

package Epl.node;

import java.util.*;
import Epl.analysis.*;

public final class ATechnique extends PTechnique
{
    private TTech _tech_;
    private TIdentifier _identifier_;
    private PBralist _bralist_;
    private TArrow _arrow_;
    private PProduct _product_;

    public ATechnique()
    {
    }

    public ATechnique(
        TTech _tech_,
        TIdentifier _identifier_,
        PBralist _bralist_,
        TArrow _arrow_,
        PProduct _product_)
    {
        setTech(_tech_);

        setIdentifier(_identifier_);

        setBralist(_bralist_);

        setArrow(_arrow_);

        setProduct(_product_);

    }
    public Object clone()
    {
        return new ATechnique(
            (TTech) cloneNode(_tech_),
            (TIdentifier) cloneNode(_identifier_),
            (PBralist) cloneNode(_bralist_),
            (TArrow) cloneNode(_arrow_),
            (PProduct) cloneNode(_product_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseATechnique(this);
    }

    public TTech getTech()
    {
        return _tech_;
    }

    public void setTech(TTech node)
    {
        if(_tech_ != null)
        {
            _tech_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _tech_ = node;
    }

    public TIdentifier getIdentifier()
    {
        return _identifier_;
    }

    public void setIdentifier(TIdentifier node)
    {
        if(_identifier_ != null)
        {
            _identifier_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _identifier_ = node;
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

    public TArrow getArrow()
    {
        return _arrow_;
    }

    public void setArrow(TArrow node)
    {
        if(_arrow_ != null)
        {
            _arrow_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _arrow_ = node;
    }

    public PProduct getProduct()
    {
        return _product_;
    }

    public void setProduct(PProduct node)
    {
        if(_product_ != null)
        {
            _product_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        _product_ = node;
    }

    public String toString()
    {
        return ""
            + toString(_tech_)
            + toString(_identifier_)
            + toString(_bralist_)
            + toString(_arrow_)
            + toString(_product_);
    }

    void removeChild(Node child)
    {
        if(_tech_ == child)
        {
            _tech_ = null;
            return;
        }

        if(_identifier_ == child)
        {
            _identifier_ = null;
            return;
        }

        if(_bralist_ == child)
        {
            _bralist_ = null;
            return;
        }

        if(_arrow_ == child)
        {
            _arrow_ = null;
            return;
        }

        if(_product_ == child)
        {
            _product_ = null;
            return;
        }

    }

    void replaceChild(Node oldChild, Node newChild)
    {
        if(_tech_ == oldChild)
        {
            setTech((TTech) newChild);
            return;
        }

        if(_identifier_ == oldChild)
        {
            setIdentifier((TIdentifier) newChild);
            return;
        }

        if(_bralist_ == oldChild)
        {
            setBralist((PBralist) newChild);
            return;
        }

        if(_arrow_ == oldChild)
        {
            setArrow((TArrow) newChild);
            return;
        }

        if(_product_ == oldChild)
        {
            setProduct((PProduct) newChild);
            return;
        }

    }
}