package org.drools.base.dataproviders;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.drools.base.mvel.DroolsMVELFactory;
import org.drools.base.mvel.MVELCompilationUnit;
import org.drools.base.mvel.MVELCompileable;
import org.drools.core.util.ArrayIterator;
import org.drools.rule.Declaration;
import org.drools.WorkingMemory;
import org.drools.spi.DataProvider;
import org.drools.spi.PropagationContext;
import org.drools.spi.Tuple;
import org.mvel2.MVEL;

public class MVELDataProvider
    implements
    DataProvider,
    MVELCompileable,
    Externalizable {

    private static final long       serialVersionUID = 1901006343031798173L;

    private MVELCompilationUnit     unit;
    private String                  id;

    private Serializable            expr;
    private DroolsMVELFactory       prototype;

    private transient Declaration[] requiredDeclarations;

    public MVELDataProvider() {

    }

    public MVELDataProvider(final MVELCompilationUnit unit,
                            final String id) {
        this.unit = unit;
        this.id = id;
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        id = in.readUTF();
        unit = (MVELCompilationUnit) in.readObject();
        //        expr    = (Serializable)in.readObject();
        //        prototype   = (DroolsMVELFactory)in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF( id );
        out.writeObject( unit );
        //        out.writeObject(expr);
        //        out.writeObject(prototype);
    }

    @SuppressWarnings("unchecked")
    public void compile(ClassLoader classLoader) {
        expr = unit.getCompiledExpression( classLoader );
        prototype = unit.getFactory();
        Map previousDeclarations = this.unit.getFactory().getPreviousDeclarations();
        this.requiredDeclarations = (Declaration[]) previousDeclarations.values().toArray( new Declaration[previousDeclarations.size()] );
    }

    public Declaration[] getRequiredDeclarations() {
        //return new Declaration[]{};
        return this.requiredDeclarations;
    }

    public void replaceDeclaration(Declaration declaration,
                                   Declaration resolved) {
        for ( int i = 0; i < this.requiredDeclarations.length; i++ ) {
            if ( this.requiredDeclarations[i].equals( declaration ) ) {
                this.requiredDeclarations[i] = resolved;
            }
        }
        this.unit.replaceDeclaration( declaration,
                                      resolved );
        prototype = unit.getFactory();
    }

    public Object createContext() {
        return this.prototype.clone();
    }

    public Iterator getResults(final Tuple tuple,
                               final WorkingMemory wm,
                               final PropagationContext ctx,
                               final Object executionContext) {
        DroolsMVELFactory factory = (DroolsMVELFactory) executionContext;

        factory.setContext( tuple,
                            null,
                            null,
                            wm,
                            null );

        //this.expression.
        final Object result = MVEL.executeExpression( this.expr,
                                                      factory );

        if ( result == null ) {
            return Collections.EMPTY_LIST.iterator();
        } else if ( result instanceof Collection ) {
            return ((Collection) result).iterator();
        } else if ( result instanceof Iterator ) {
            return (Iterator) result;
        } else if ( result.getClass().isArray() ) {
            return new ArrayIterator( result );
        } else {
            return Collections.singletonList( result ).iterator();
        }
    }

    public DataProvider clone() {
        // not sure this is safe, but at this point we don't have a classloader
        // reference to compile a new copy of the data provider. My require
        // refactory later.
        return this;
    }
}
