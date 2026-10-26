/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2020, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.theoneprobe;

import com.google.common.base.Function;

import mcjty.theoneprobe.api.ITheOneProbe;

public class TheOneProbeModule implements Function<ITheOneProbe, Void> {

    @Override
    public Void apply(ITheOneProbe input) {
        // 1.12.2: The One Probe builds the probe info on the server and only knows plain strings, the text
        // components of AE2 are sent as an element of their own and translated on the client
        TextComponentElement.setElementId(input.registerElementFactory(TextComponentElement::new));

        input.registerProbeConfigProvider(new AEConfigProvider());

        var provider = new BlockEntityInfoProvider();
        input.registerProvider(provider);
        input.registerBlockDisplayOverride(provider);

        return null;
    }
}
