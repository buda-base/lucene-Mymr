# lucene-Mymr: Myanmar script analyzer for Lucene

This repository contains some Myanmar-specific functions for a Lucene index to handle Myanmar script.

It does not provide a tokenizer (we assume users will want to use the Lucene ICU tokenizer), but provides a few tests.

It provides Unicode character reordering (based on the one used in font layout engines), and a few Unicode normalizations.
