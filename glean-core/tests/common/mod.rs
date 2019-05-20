// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

use glean_core::Glean;

pub fn tempdir() -> (tempfile::TempDir, String) {
    let t = tempfile::tempdir().unwrap();
    let name = t.path().display().to_string();
    (t, name)
}

pub const GLOBAL_APPLICATION_ID: &str = "org.mozilla.glean.test.app";

// Create a new instance of Glean with a temporary directory.
// We need to keep the `TempDir` alive, so that it's not deleted before we stop using it.
pub fn new_glean() -> (Glean, tempfile::TempDir) {
    let dir = tempfile::tempdir().unwrap();
    let tmpname = dir.path().display().to_string();

    let glean = Glean::new(&tmpname, GLOBAL_APPLICATION_ID).unwrap();

    (glean, dir)
}
