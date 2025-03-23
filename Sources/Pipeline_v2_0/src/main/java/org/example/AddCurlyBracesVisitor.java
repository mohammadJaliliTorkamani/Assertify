package org.example;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class AddCurlyBracesVisitor extends ModifierVisitor<Void> {
    @Override
    public Visitable visit(IfStmt n, Void arg) {
        n.setThenStmt(wrapInBlockStmt(n.getThenStmt()));
        n.getElseStmt().ifPresent(stmt -> n.setElseStmt(wrapInBlockStmt(stmt)));
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(ForEachStmt n, Void arg) {
        n.setBody(wrapInBlockStmt(n.getBody()));
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(ForStmt n, Void arg) {
        n.setBody(wrapInBlockStmt(n.getBody()));
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(WhileStmt n, Void arg) {
        n.setBody(wrapInBlockStmt(n.getBody()));
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(DoStmt n, Void arg) {
        n.setBody(wrapInBlockStmt(n.getBody()));
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(BlockStmt n, Void arg) {
        n.getStatements().forEach(stmt -> stmt.accept(this, arg));
        return super.visit(n, arg);
    }

    private BlockStmt wrapInBlockStmt(Statement stmt) {
        if (stmt instanceof BlockStmt) {
            return (BlockStmt) stmt;
        }
        return new BlockStmt().addStatement(stmt);
    }
}
