package sqlancer.duckdb.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.ast.newast.Node;
import sqlancer.ast.newast.TableReferenceNode;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.DuckDBSchema.DuckDBColumn;
import sqlancer.duckdb.DuckDBSchema.DuckDBTable;
import sqlancer.duckdb.gen.DuckDBExpressionGenerator;

public class DuckDBJoin implements Node<DuckDBExpression> {

	private final TableReferenceNode<DuckDBExpression, DuckDBTable> leftTable;
	private final TableReferenceNode<DuckDBExpression, DuckDBTable> rightTable;
	private final JoinType joinType;
	private final Node<DuckDBExpression> onCondition;
	private OuterType outerType;

	public static enum JoinType {
		INNER, NATURAL, LEFT, RIGHT;

		public static JoinType getRandom() {
			return Randomly.fromOptions(values());
		}
	}

	public static enum OuterType {
		FULL, LEFT, RIGHT;

		public static OuterType getRandom() {
			return Randomly.fromOptions(values());
		}
	}

	public DuckDBJoin(TableReferenceNode<DuckDBExpression, DuckDBTable> leftTable,
			TableReferenceNode<DuckDBExpression, DuckDBTable> rightTable, JoinType joinType,
			Node<DuckDBExpression> whereCondition) {
		this.leftTable = leftTable;
		this.rightTable = rightTable;
		this.joinType = joinType;
		this.onCondition = whereCondition;
	}

	public TableReferenceNode<DuckDBExpression, DuckDBTable> getLeftTable() {
		return leftTable;
	}

	public TableReferenceNode<DuckDBExpression, DuckDBTable> getRightTable() {
		return rightTable;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public Node<DuckDBExpression> getOnCondition() {
		return onCondition;
	}

	private void setOuterType(OuterType outerType) {
		this.outerType = outerType;
	}

	public OuterType getOuterType() {
		return outerType;
	}

	public static List<Node<DuckDBExpression>> getJoins(
			List<TableReferenceNode<DuckDBExpression, DuckDBTable>> tableList, DuckDBGlobalState globalState) {
		List<Node<DuckDBExpression>> joinExpressions = new ArrayList<>();
		while (tableList.size() >= 2 && Randomly.getBoolean()) {
			TableReferenceNode<DuckDBExpression, DuckDBTable> leftTable = tableList.remove(0);
			TableReferenceNode<DuckDBExpression, DuckDBTable> rightTable = tableList.remove(0);
			List<DuckDBColumn> columns = new ArrayList<>(leftTable.getTable().getColumns());
			columns.addAll(rightTable.getTable().getColumns());
			DuckDBExpressionGenerator joinGen = new DuckDBExpressionGenerator(globalState).setColumns(columns);
			switch (DuckDBJoin.JoinType.getRandom()) {
			case INNER:
				joinExpressions.add(DuckDBJoin.createInnerJoin(leftTable, rightTable, joinGen.generateExpression()));
				break;
			case NATURAL:
				joinExpressions.add(DuckDBJoin.createNaturalJoin(leftTable, rightTable, OuterType.getRandom()));
				break;
			case LEFT:
				joinExpressions
						.add(DuckDBJoin.createLeftOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
				if (true) {
					// https://github.com/cwida/duckdb/issues/508
					throw new IgnoreMeException();
				}
				break;
			case RIGHT:
				if (true) {
					// https://github.com/cwida/duckdb/issues/505
					throw new IgnoreMeException();
				}
				joinExpressions
						.add(DuckDBJoin.createRightOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
				break;
			default:
				throw new AssertionError();
			}
		}
		return joinExpressions;
	}

	public static DuckDBJoin createRightOuterJoin(TableReferenceNode<DuckDBExpression, DuckDBTable> left,
			TableReferenceNode<DuckDBExpression, DuckDBTable> right, Node<DuckDBExpression> predicate) {
		DuckDBJoin duckDBJoin = new DuckDBJoin(left, right, JoinType.RIGHT, predicate);
		return duckDBJoin;
	}

	public static DuckDBJoin createLeftOuterJoin(TableReferenceNode<DuckDBExpression, DuckDBTable> left,
			TableReferenceNode<DuckDBExpression, DuckDBTable> right, Node<DuckDBExpression> predicate) {
		DuckDBJoin duckDBJoin = new DuckDBJoin(left, right, JoinType.LEFT, predicate);
		return duckDBJoin;
	}

	public static DuckDBJoin createInnerJoin(TableReferenceNode<DuckDBExpression, DuckDBTable> left,
			TableReferenceNode<DuckDBExpression, DuckDBTable> right, Node<DuckDBExpression> predicate) {
		return new DuckDBJoin(left, right, JoinType.INNER, predicate);
	}

	public static Node<DuckDBExpression> createNaturalJoin(TableReferenceNode<DuckDBExpression, DuckDBTable> left,
			TableReferenceNode<DuckDBExpression, DuckDBTable> right, OuterType naturalJoinType) {
		DuckDBJoin join = new DuckDBJoin(left, right, JoinType.NATURAL, null);
		join.setOuterType(naturalJoinType);
		return join;
	}

}
