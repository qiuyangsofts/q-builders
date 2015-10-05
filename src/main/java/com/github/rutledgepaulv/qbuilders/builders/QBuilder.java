package com.github.rutledgepaulv.qbuilders.builders;

import com.github.rutledgepaulv.qbuilders.conditions.CompleteCondition;
import com.github.rutledgepaulv.qbuilders.conditions.PartialCondition;
import com.github.rutledgepaulv.qbuilders.nodes.*;
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator;
import com.github.rutledgepaulv.qbuilders.properties.concrete.*;
import com.github.rutledgepaulv.qbuilders.properties.virtual.*;
import com.github.rutledgepaulv.qbuilders.utilities.VarArgUtils;
import com.github.rutledgepaulv.qbuilders.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class QBuilder<T extends QBuilder> implements PartialCondition<T> {

    protected LogicalNode root;
    protected LogicalNode current;

    public QBuilder() {
        root = new OrNode(null, new ArrayList<>());
        current = root;
    }

    public BooleanProperty<T> booleanField(String field) {
        return new BooleanPropertyDelegate(field, self());
    }

    public StringProperty<T> stringField(String field) {
        return new StringPropertyDelegate(field, self());
    }

    public ShortProperty<T> shortField(String field) {
        return new ShortPropertyDelegate(field, self());
    }

    public IntegerProperty<T> integerField(String field) {
        return new IntegerPropertyDelegate(field, self());
    }

    public LongProperty<T> longField(String field) {
        return new LongPropertyDelegate(field, self());
    }

    public FloatProperty<T> floatField(String field) {
        return new FloatPropertyDelegate(field, self());
    }

    public DoubleProperty<T> doubleField(String field) {
        return new DoublePropertyDelegate(field, self());
    }

    @SafeVarargs
    public final CompleteCondition<T> and(CompleteCondition<T> c1, CompleteCondition<T> c2,
            CompleteCondition<T>... cn) {
        return and(VarArgUtils.combine(c1, c2, cn));
    }

    @SafeVarargs
    public final CompleteCondition<T> or(CompleteCondition<T> c1, CompleteCondition<T> c2, CompleteCondition<T>... cn) {
        return or(VarArgUtils.combine(c1, c2, cn));
    }

    public final CompleteCondition<T> and(List<CompleteCondition<T>> completeConditions) {

        List<AbstractNode> children = completeConditions.stream()
                .map(condition -> ((QBuilder<T>) condition).self().current).collect(Collectors.toList());

        AndNode node = new AndNode(self().current, children);
        self().current.getChildren().add(node);

        return new CompleteConditionDelegate(self());
    }

    public final CompleteCondition<T> or(List<CompleteCondition<T>> completeConditions) {

        List<AbstractNode> children = completeConditions.stream()
                .map(condition -> ((QBuilder<T>) condition).self().current).collect(Collectors.toList());

        OrNode node = new OrNode(self().current, children);
        self().current.getChildren().add(node);

        return new CompleteConditionDelegate(self());
    }

    protected CompleteCondition<T> condition(String field, ComparisonOperator operator, Collection<?> values) {
        self().current.getChildren().add(new ComparisonNode(self().current, field, operator, values));
        return new CompleteConditionDelegate(self());
    }

    protected T self() {
        return (T) this;
    }

    private abstract class Delegate extends QBuilder<T> {
        private T canonical;

        public Delegate(T canonical) {
            this.canonical = canonical;
        }

        @Override
        protected T self() {
            return canonical;
        }
    }

    private class CompleteConditionDelegate extends Delegate implements CompleteCondition<T> {

        public CompleteConditionDelegate(T canonical) {
            super(canonical);
        }

        public T and() {
            LogicalNode current = self().current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            AndNode node = new AndNode(current.getParent(), children);
            if (current == self().root) {
                self().root = node;
            }
            self().current = node;
            return self();
        }

        public T or() {
            LogicalNode current = self().current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            OrNode node = new OrNode(current.getParent(), children);
            if (current == self().root) {
                self().root = node;
            }
            self().current = node;
            return self();
        }

        public <Q> Q query(NodeVisitor<Q> visitor) {
            return self().root.visit(visitor);
        }

    }

    private abstract class PropertyDelegate extends Delegate implements Property<T> {

        private String field;

        public PropertyDelegate(String field, T canonical) {
            super(canonical);
            this.field = field;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    private abstract class ExistentialPropertyDelegate extends PropertyDelegate implements ExistentialProperty<T> {

        public ExistentialPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> exists() {
            return condition(getField(), ComparisonOperator.EX, Collections.singletonList(true));
        }

        public CompleteCondition<T> doesNotExist() {
            return condition(getField(), ComparisonOperator.EX, Collections.singletonList(false));
        }

    }

    private abstract class EquitablePropertyDelegate<S> extends ExistentialPropertyDelegate
            implements EquitableProperty<T, S> {

        public EquitablePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> eq(S value) {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(value));
        }

        public CompleteCondition<T> ne(S value) {
            return condition(getField(), ComparisonOperator.NE, Collections.singletonList(value));
        }
    }

    private abstract class ListablePropertyDelegate<S> extends EquitablePropertyDelegate<S>
            implements ListableProperty<T, S> {

        public ListablePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> in(S... values) {
            return condition(getField(), ComparisonOperator.IN, VarArgUtils.list(values));
        }

        public CompleteCondition<T> in(Collection<S> values) {
            return condition(getField(), ComparisonOperator.IN, values);
        }

        public CompleteCondition<T> nin(S... values) {
            return condition(getField(), ComparisonOperator.NIN, VarArgUtils.list(values));
        }

        public CompleteCondition<T> nin(Collection<S> values) {
            return condition(getField(), ComparisonOperator.NIN, values);
        }
    }

    private class NumberPropertyDelegate<S extends Number> extends ListablePropertyDelegate<S>
            implements NumberProperty<T, S> {

        public NumberPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> gt(S number) {
            return condition(getField(), ComparisonOperator.GT, Collections.singletonList(number));
        }

        public CompleteCondition<T> lt(S number) {
            return condition(getField(), ComparisonOperator.LT, Collections.singletonList(number));
        }

        public CompleteCondition<T> gte(S number) {
            return condition(getField(), ComparisonOperator.GTE, Collections.singletonList(number));
        }

        public CompleteCondition<T> lte(S number) {
            return condition(getField(), ComparisonOperator.LTE, Collections.singletonList(number));
        }

    }

    private class BooleanPropertyDelegate extends ExistentialPropertyDelegate implements BooleanProperty<T> {

        public BooleanPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> isTrue() {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(true));
        }

        public CompleteCondition<T> isFalse() {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(false));
        }

    }

    private class ShortPropertyDelegate extends NumberPropertyDelegate<Short> implements ShortProperty<T> {

        public ShortPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private class IntegerPropertyDelegate extends NumberPropertyDelegate<Integer> implements IntegerProperty<T> {

        public IntegerPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private class LongPropertyDelegate extends NumberPropertyDelegate<Long> implements LongProperty<T> {

        public LongPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private class FloatPropertyDelegate extends NumberPropertyDelegate<Float> implements FloatProperty<T> {

        public FloatPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private class DoublePropertyDelegate extends NumberPropertyDelegate<Double> implements DoubleProperty<T> {

        public DoublePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private class StringPropertyDelegate extends ListablePropertyDelegate<String> implements StringProperty<T> {

        public StringPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public CompleteCondition<T> lexicallyAfter(String value) {
            return condition(getField(), ComparisonOperator.GT, Collections.singletonList(value));
        }

        public CompleteCondition<T> lexicallyBefore(String value) {
            return condition(getField(), ComparisonOperator.LT, Collections.singletonList(value));
        }

    }

}