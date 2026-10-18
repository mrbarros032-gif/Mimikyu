# AI Engineering Rules & Constraints

## 1. Zero-Regression Policy (CRITICAL)
- **SURGICAL EDITS ONLY:** Never rewrite an entire file if only 10 lines need changing. Modify only the exact functions, hooks, or styles related to the request.
- **DO NOT TOUCH WORKING TOOLS:** Leave existing imports, helper functions, state variables, and unrelated UI components untouched unless explicitly asked to refactor them.
- **READ BEFORE WRITING:** Before modifying any function or component, trace where it is called across the codebase to ensure you do not break props, return types, or side effects.

## 2. Anti-Slop & Code Quality
- **NO PLACEHOLDERS:** Never output `// TODO`, `// ... rest of code here`, or stubbed functions. Every code block must be 100% complete and functional.
- **NO HALLUCINATED PACKAGES:** Only use libraries already present in `package.json` / `requirements.txt` / build scripts. Do not invent utility functions that do not exist in the codebase.
- **NO OVER-ENGINEERING:** Write simple, readable, direct code. Do not create unnecessary wrapper classes, abstract factories, or complex custom hooks for one-off problems.
- **NO YAPPING:** Skip introductory apologies, motivational filler, and obvious explanations. State the root cause in 1-2 sentences, then provide the exact code change.

## 3. UI & Performance Optimization Standards
- **PREVENT UNNECESSARY RE-RENDERS:** Ensure event handlers, state updates, and effects do not trigger infinite loops or heavy re-renders on every keystroke/frame.
- **CLEAN UP SIDE EFFECTS:** Every event listener, timer, subscription, or async call in a component must have a proper cleanup/abort mechanism.
- **HANDLE EDGE STATES:** Every UI interaction must account for loading, error, empty, and rapid-click (debouncing/disabled) states.

## 4. Mandatory Debugging Workflow
- **NEVER SAY "NO BUGS FOUND":** If the user reports broken behavior or laggy UI, a bug exists. Static syntax being valid does not mean runtime logic is correct.
- **ROOT CAUSE FIRST:** Before outputting a fix, briefly state:
  1. What exact state/lifecycle/logic flaw is causing the issue.
  2. Which exact lines will be changed.
  3. Why this change will not break adjacent features.
