package com.seleniumboot.locator;

/**
 * JavaScript snippets and shared CSS selectors used by {@link Locator} internals.
 * Package-private implementation detail of the locator package.
 */
final class LocatorJs {

    private LocatorJs() {
    }

    /** Records whether a native drag actually delivered a {@code drop} event to the target. */
    static final String ARM_DROP_LISTENER_JS =
            "var t = arguments[0]; t.__sbDropped = false;"
            + "t.__sbDropHandler = function () { t.__sbDropped = true; };"
            + "t.addEventListener('drop', t.__sbDropHandler);";

    static final String COLLECT_DROP_RESULT_JS =
            "var t = arguments[0]; t.removeEventListener('drop', t.__sbDropHandler);"
            + "var dropped = t.__sbDropped === true; delete t.__sbDropped; delete t.__sbDropHandler;"
            + "return dropped;";

    /** HTML5 DnD fallback: replays the event sequence with one shared DataTransfer. */
    static final String SYNTHETIC_DRAG_JS =
            "var src = arguments[0], dst = arguments[1], dt = new DataTransfer();"
            + "function fire(el, type) {"
            + "  var r = el.getBoundingClientRect();"
            + "  el.dispatchEvent(new DragEvent(type, {bubbles: true, cancelable: true, composed: true,"
            + "    dataTransfer: dt, clientX: r.left + r.width / 2, clientY: r.top + r.height / 2}));"
            + "}"
            + "fire(src, 'dragstart'); fire(dst, 'dragenter'); fire(dst, 'dragover');"
            + "fire(dst, 'drop'); fire(src, 'dragend');";

    /** Computes an element's accessible name, following the common ARIA precedence. */
    static final String ACCESSIBLE_NAME_JS =
            "const e = arguments[0];"
            + "if(!e) return '';"
            + "const al = e.getAttribute && e.getAttribute('aria-label');"
            + "if(al && al.trim()) return al.trim();"
            + "const lb = e.getAttribute && e.getAttribute('aria-labelledby');"
            + "if(lb){const t = lb.split(/\\s+/).map(function(id){var r=document.getElementById(id);return r?r.textContent:'';}).join(' ').trim(); if(t) return t;}"
            + "if(e.labels && e.labels.length){return Array.prototype.map.call(e.labels,function(l){return l.textContent;}).join(' ').trim();}"
            + "const id = e.getAttribute && e.getAttribute('id');"
            + "if(id){try{var lab=document.querySelector('label[for=\"'+id+'\"]'); if(lab) return lab.textContent.trim();}catch(x){}}"
            + "const wrap = e.closest && e.closest('label'); if(wrap) return wrap.textContent.trim();"
            + "const txt = (e.textContent||'').trim(); if(txt) return txt;"
            + "const alt = e.getAttribute && e.getAttribute('alt'); if(alt && alt.trim()) return alt.trim();"
            + "const ti = e.getAttribute && e.getAttribute('title'); if(ti && ti.trim()) return ti.trim();"
            + "const val = e.value; if(val) return (''+val).trim();"
            + "const ph = e.getAttribute && e.getAttribute('placeholder'); if(ph && ph.trim()) return ph.trim();"
            + "return '';";

    /** Form controls that can carry an accessible name from an associated label. */
    static final String FORM_CONTROL_CSS =
            "input:not([type='hidden']):not([type='button']):not([type='submit']):not([type='reset']), "
            + "textarea, select, [contenteditable='true'], "
            + "[role='textbox'], [role='searchbox'], [role='combobox'], [role='checkbox'], "
            + "[role='radio'], [role='switch'], [role='slider'], [role='spinbutton']";
}
