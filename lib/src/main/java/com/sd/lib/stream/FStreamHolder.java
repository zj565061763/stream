package com.sd.lib.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

class FStreamHolder
{
    private final Collection<FStream> mStreamHolder = new LinkedHashSet<>();

    public boolean add(FStream stream)
    {
        return mStreamHolder.add(stream);
    }

    public boolean remove(FStream stream)
    {
        return mStreamHolder.remove(stream);
    }

    public int size()
    {
        return mStreamHolder.size();
    }

    public boolean isEmpty()
    {
        return mStreamHolder.isEmpty();
    }

    public Collection<FStream> sort(Comparator<FStream> comparator)
    {
        if (comparator == null)
            return null;

        final List<FStream> listEntry = new ArrayList<>(mStreamHolder);
        Collections.sort(listEntry, comparator);

        mStreamHolder.clear();
        mStreamHolder.addAll(listEntry);

        return listEntry;
    }

    public Collection<FStream> toCollection()
    {
        return new ArrayList<>(mStreamHolder);
    }
}
