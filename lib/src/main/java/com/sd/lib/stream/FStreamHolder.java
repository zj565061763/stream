package com.sd.lib.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

class FStreamHolder
{
    private final Class<? extends FStream> mClass;
    private final Collection<FStream> mStreamHolder = new LinkedHashSet<>();

    public FStreamHolder(Class<? extends FStream> clazz)
    {
        mClass = clazz;
    }

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

    public Collection<FStream> sort()
    {
        final List<FStream> listEntry = new ArrayList<>(mStreamHolder);
        Collections.sort(listEntry, new InternalStreamComparator());

        mStreamHolder.clear();
        mStreamHolder.addAll(listEntry);

        return listEntry;
    }

    public Collection<FStream> toCollection()
    {
        return new ArrayList<>(mStreamHolder);
    }

    private FStreamManager getManager()
    {
        return FStreamManager.getInstance();
    }

    private final class InternalStreamComparator implements Comparator<FStream>
    {
        @Override
        public int compare(FStream o1, FStream o2)
        {
            final StreamConnection o1Connection = getManager().getConnection(o1);
            final StreamConnection o2Connection = getManager().getConnection(o2);
            if (o1Connection != null && o2Connection != null)
            {
                return o2Connection.getPriority(mClass) - o1Connection.getPriority(mClass);
            }
            return 0;
        }
    }
}
