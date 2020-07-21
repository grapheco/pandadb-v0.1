/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import org.junit.Test;

import org.neo4j.csv.reader.Extractors.IntExtractor;
import org.neo4j.csv.reader.Extractors.BlobExtractor;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.PointValue;
import org.neo4j.values.storable.Values;
import org.neo4j.csv.reader.Extractors;
import org.neo4j.csv.reader.Extractor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ExtractorsTest
{


    @Test
    public void shouldExtractBlob()
    {
        // GIVEN
        Extractors extractors = new Extractors( ',' );
        String value = "<http://s12.sinaimg.cn/mw690/005AE7Quzy7rL8kA4Nt6b&690>";

        // WHEN
        char[] asChars = value.toCharArray();
        BlobExtractor extractor = extractors.blob();
        extractor.extract( asChars, 0, asChars.length, false );

        // THEN
        System.out.println(extractor.value());
    }

}
