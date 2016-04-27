/*
 * Copyright 2015-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.nebula.lint.analyzer

import org.codehaus.groovy.ast.ASTNode
import org.codenarc.analyzer.SuppressionAnalyzer
import org.codenarc.source.AbstractSourceCode

class CorrectableStringSource extends AbstractSourceCode {
    String originalSource
    List<String> lines

    Map<ASTNode, String> replacements = [:]
    List<ASTNode> deletions = []
    Map<ASTNode, List<String>> additions = [:].withDefault {[]}

    CorrectableStringSource(String source) {
        assert source != null
        this.originalSource = source
        this.lines = new StringReader(source).readLines()
        setSuppressionAnalyzer(new SuppressionAnalyzer(this))
    }

    String getCorrectedSource() {
        def corrections = new StringBuffer()
        def lastLineDeleted = false

        for(int i = 0; i < lines.size(); i++) {
            if(i > 0 && !lastLineDeleted)
                corrections.append('\n')

            lastLineDeleted = false

            def replacement = replacements.find { it.key.lineNumber-1 == i }
            def deletion = deletions.find { it.lineNumber-1 == i }
            def additions = additions.find { it.key.lineNumber-1 == i }

            if(replacement) {
                // FIXME multiple replacements on the same line
                corrections.append(doReplacement(replacement.key, replacement.value))
                i += replacement.key.lastLineNumber - replacement.key.lineNumber
            } else if(deletion) {
                i += deletion.lastLineNumber-deletion.lineNumber
                lastLineDeleted = true
            } else {
                corrections.append(lines[i])
            }

            if(additions) {
                additions.value.each {
                    corrections.append(it)
                }
            }
        }

        if(originalSource.endsWith('\n'))
            corrections.append('\n')

        corrections.toString()
    }

    private String doReplacement(ASTNode node, String replacement) {
        // note that node line and column numbers are both 1 based
        def linesToReplace = lines.subList(node.lineNumber-1, node.lastLineNumber)

        def lastColumn = node.lastColumnNumber-1
        if(linesToReplace.size() > 1)
            lastColumn += linesToReplace[0..-2].sum { it.length()+1 } // +1 for the extra newline character we are going to add

        def allLines = linesToReplace.join('\n')

        allLines.substring(0, node.columnNumber-1) + replacement + allLines.substring(lastColumn)
    }

    void replace(ASTNode node, String replacement) {
        this.replacements[node] = replacement
    }

    void delete(ASTNode node) {
        this.deletions += node
    }

    void add(ASTNode node, String addition) {
        this.additions[node] += addition
    }

    @Override
    String getText() {
        lines.join('\n')
    }

    @Override
    List<String> getLines() {
        lines
    }

    @Override
    String toString() {
        "CorrectableSourceString[$text]"
    }

    @Override
    String getName() {
        return null
    }

    @Override
    String getPath() {
        return null
    }
}